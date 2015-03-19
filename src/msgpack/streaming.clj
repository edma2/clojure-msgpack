(ns msgpack.streaming
  (:require [msgpack.io :refer :all])
  (:import java.io.DataOutputStream
           java.io.ByteArrayOutputStream
           java.io.DataInputStream
           java.io.ByteArrayInputStream
           java.nio.charset.Charset))

(defprotocol Packable
  "Objects that can be serialized as MessagePack types"
  (pack-stream [this data-output]))

;; MessagePack allows applications to define application-specific types using
;; the Extended type. Extended type consists of an integer and a byte array
;; where the integer represents a kind of types and the byte array represents
;; data.
(defrecord Extended [type data])

(defmacro cond-let [bindings & clauses]
  `(let ~bindings (cond ~@clauses)))

(declare pack-number pack-bytes pack-float pack-coll)

(extend-protocol Packable
  nil
  (pack-stream
    [_ s]
    (.writeByte s 0xc0))

  Boolean
  (pack-stream
    [bool s]
    (if bool
      (.writeByte s 0xc3)
      (.writeByte s 0xc2)))

  Byte
  (pack-stream [n s] (pack-number n s))

  Short
  (pack-stream [n s] (pack-number n s))

  Integer
  (pack-stream [n s] (pack-number n s))

  Long
  (pack-stream [n s] (pack-number n s))

  clojure.lang.BigInt
  (pack-stream [n s] (pack-number n s))

  Float
  (pack-stream [f s] (pack-float f s))

  Double
  (pack-stream [d s] (pack-float d s))

  clojure.lang.Ratio
  (pack-stream
    [r s]
    (pack-stream (double r) s))

  String
  (pack-stream
    [str s]
    (cond-let [bytes (.getBytes str (Charset/forName "UTF-8"))
               len (count bytes)]
              (<= len 0x1f)
              (do (.writeByte s (bit-or 2r10100000 len)) (.write s bytes))

              (<= len 0xff)
              (do (.writeByte s 0xd9) (.writeByte s len) (.write s bytes))

              (<= len 0xffff)
              (do (.writeByte s 0xda) (.writeShort s len) (.write s bytes))

              (<= len 0xffffffff)
              (do (.writeByte s 0xdb) (.writeInt s len) (.write s bytes))))

  clojure.lang.Keyword
  (pack-stream [kw s] (pack-stream (name kw) s))

  clojure.lang.Symbol
  (pack-stream [sym s] (pack-stream (name sym) s))

  Extended
  (pack-stream
    [e s]
    (let [type (:type e)
          data (byte-array (:data e))
          len (count data)]
      (do
        (cond
          (= len 1) (.writeByte s 0xd4)
          (= len 2) (.writeByte s 0xd5)
          (= len 4) (.writeByte s 0xd6)
          (= len 8) (.writeByte s 0xd7)
          (= len 16) (.writeByte s 0xd8)
          (<= len 0xff) (do (.writeByte s 0xc7) (.writeByte s len))
          (<= len 0xffff) (do (.writeByte s 0xc8) (.writeShort s len))
          (<= len 0xffffffff) (do (.writeByte s 0xc9) (.writeInt s len)))
        (.writeByte s type)
        (.write s data))))

  clojure.lang.Sequential
  (pack-stream [seq s]
    (cond-let [len (count seq)]
              (<= len 0xf)
              (do (.writeByte s (bit-or 2r10010000 len)) (pack-coll seq s))

              (<= len 0xffff)
              (do (.writeByte s 0xdc) (.writeShort s len) (pack-coll seq s))

              (<= len 0xffffffff)
              (do (.writeByte s 0xdd) (.writeInt s len) (pack-coll seq s))))

  ; TODO: try to deserialize keys as keywords
  clojure.lang.IPersistentMap
  (pack-stream [map s]
    (cond-let [len (count map)
               pairs (interleave (keys map) (vals map))]
              (<= len 0xf)
              (do (.writeByte s (bit-or 2r10000000 len)) (pack-coll pairs s))

              (<= len 0xffff)
              (do (.writeByte s 0xde) (.writeShort s len) (pack-coll pairs s))

              (<= len 0xffffffff)
              (do (.writeByte s 0xdf) (.writeInt s len) (pack-coll pairs s))))

  ; TODO: serialize set as a map with values equal to true (boolean)
  ; If we deserialize a map with all values equal to true, then
  ; automatically deserialize as a set.
  clojure.lang.IPersistentSet
  (pack-stream [set s] (pack-stream (sequence set) s)))

; Note: the extensions below are not in extend-protocol above because of
; a Clojure bug. See http://dev.clojure.org/jira/browse/CLJ-1381

; Array of java.lang.Byte (boxed)
(extend-type (class (java.lang.reflect.Array/newInstance Byte 0))
  Packable
  (pack-stream [bytes s] (pack-bytes bytes s)))

; Array of primitive bytes (un-boxed)
(extend-type (Class/forName "[B")
  Packable
  (pack-stream [bytes s] (pack-bytes bytes s)))

(defn- pack-bytes
  [bytes s]
  (cond-let [len (count bytes)]
            (<= len 0xff)
            (do (.writeByte s 0xc4) (.writeByte s len) (.write s bytes))

            (<= len 0xffff)
            (do (.writeByte s 0xc5) (.writeShort s len) (.write s bytes))

            (<= len 0xffffffff)
            (do (.writeByte s 0xc6) (.writeInt s len) (.write s bytes))))

(defn- pack-number
  "Pack n using the most compact representation"
  [n s]
  (cond
    ; +fixnum
    (<= 0 n 127)                  (.writeByte s n)
    ; -fixnum
    (<= -32 n -1)                 (.writeByte s n)
    ; uint 8
    (<= 0 n 0xff)                 (do (.writeByte s 0xcc) (.writeByte s n))
    ; uint 16
    (<= 0 n 0xffff)               (do (.writeByte s 0xcd) (.writeShort s n))
    ; uint 32
    (<= 0 n 0xffffffff)           (do (.writeByte s 0xce) (.writeInt s n))
    ; uint 64
    (<= 0 n 0xffffffffffffffff)   (do (.writeByte s 0xcf) (.writeLong s n))
    ; int 8
    (<= -0x80 n -1)               (do (.writeByte s 0xd0) (.writeByte s n))
    ; int 16
    (<= -0x8000 n -1)             (do (.writeByte s 0xd1) (.writeShort s n))
    ; int 32
    (<= -0x80000000 n -1)         (do (.writeByte s 0xd2) (.writeInt s n))
    ; int 64
    (<= -0x8000000000000000 n -1) (do (.writeByte s 0xd3) (.writeLong s n))))

(defn- pack-float
  "Pack f using the most compact representation"
  [f s]
  (if (<= f Float/MAX_VALUE)
    (do (.writeByte s 0xca) (.writeFloat s f))
    (do (.writeByte s 0xcb) (.writeDouble s f))))

(defn- pack-coll
  [coll s]
  (doseq [item coll] (pack-stream item s)))

(defn pack [obj]
  (let [output-stream (ByteArrayOutputStream.)
        data-output (DataOutputStream. output-stream)]
    (do
      (pack-stream obj data-output)
      (seq (.toByteArray output-stream)))))

(defn- read-uint8
  [data-input]
  (.readUnsignedByte data-input))

(defn- read-uint16
  [data-input]
  (.readUnsignedShort data-input))

(defn- read-uint32
  [data-input]
  (bit-and 0xffffffff (.readInt data-input)))

(defn- read-uint64
  [data-input]
  (let [n (.readLong data-input)]
    (if (<= 0 n Long/MAX_VALUE)
      n
      (.and (biginteger n) (biginteger 0xffffffffffffffff)))))

(defn unpack-stream [data-input]
  (cond-let [ubyte (.readUnsignedByte data-input)
             sbyte (unchecked-byte ubyte)]
            (= ubyte 0xc0) nil
            (= ubyte 0xc2) false
            (= ubyte 0xc3) true
            (<= -32 sbyte 127) sbyte
            (= ubyte 0xcc) (read-uint8 data-input)
            (= ubyte 0xcd) (read-uint16 data-input)
            (= ubyte 0xce) (read-uint32 data-input)
            (= ubyte 0xcf) (read-uint64 data-input)
            (= ubyte 0xd0) (.readByte data-input)
            (= ubyte 0xd1) (.readShort data-input)
            (= ubyte 0xd2) (.readInt data-input)
            (= ubyte 0xd3) (.readLong data-input)
            (= ubyte 0xca) (.readFloat data-input)
            (= ubyte 0xcb) (.readDouble data-input)))

(defn unpack
  "Unpack bytes as MessagePack object."
  [bytes]
  (-> bytes
      byte-array
      ByteArrayInputStream.
      DataInputStream.
      unpack-stream))
