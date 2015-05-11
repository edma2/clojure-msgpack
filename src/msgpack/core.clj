(ns msgpack.core
  (:import java.io.DataOutputStream
           java.io.ByteArrayOutputStream
           java.io.DataInputStream
           java.io.ByteArrayInputStream
           java.nio.charset.Charset))

(defprotocol Packable
  "Objects that can be serialized as MessagePack types"
  (pack-stream [this data-output]))

;; MessagePack allows applications to define application-specific types using
;; the Extension type. Extension type consists of an integer and a byte array
;; where the integer represents a kind of types and the byte array represents
;; data.
;; TODO: encode as Extended type: Ratios, Keywords, Symbols, Sets, more?
(defrecord Extension [type data])

(defmacro cond-let [bindings & clauses]
  `(let ~bindings (cond ~@clauses)))

(defn- pack-bytes
  [^bytes bytes ^java.io.DataOutput s]
  (cond-let [len (count bytes)]
            (<= len 0xff)
            (do (.writeByte s 0xc4) (.writeByte s len) (.write s bytes))

            (<= len 0xffff)
            (do (.writeByte s 0xc5) (.writeShort s len) (.write s bytes))

            (<= len 0xffffffff)
            (do (.writeByte s 0xc6) (.writeInt s len) (.write s bytes))))

(defn- pack-number
  "Pack n using the most compact representation"
  [n ^java.io.DataOutput s]
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
    (<= 0 n 0xffffffff)           (do (.writeByte s 0xce) (.writeInt s (unchecked-int n)))
    ; uint 64
    (<= 0 n 0xffffffffffffffff)   (do (.writeByte s 0xcf) (.writeLong s (unchecked-long n)))
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
  [f ^java.io.DataOutput s]
  (if (<= f Float/MAX_VALUE)
    (do (.writeByte s 0xca) (.writeFloat s f))
    (do (.writeByte s 0xcb) (.writeDouble s f))))

(defn- pack-coll
  [coll ^java.io.DataOutput s]
  (doseq [item coll] (pack-stream item s)))

(extend-protocol Packable
  nil
  (pack-stream
    [_ ^java.io.DataOutput s]
    (.writeByte s 0xc0))

  Boolean
  (pack-stream
    [bool ^java.io.DataOutput s]
    (if bool
      (.writeByte s 0xc3)
      (.writeByte s 0xc2)))

  ;; TODO: use derive

  Float
  (pack-stream [f ^java.io.DataOutput s] (pack-float f s))

  Double
  (pack-stream [d ^java.io.DataOutput s] (pack-float d s))

  java.math.BigDecimal
  (pack-stream [d ^java.io.DataOutput s] (pack-float d s))

  Number
  (pack-stream [n ^java.io.DataOutput s] (pack-number n s))

  clojure.lang.Ratio
  (pack-stream
    [r ^java.io.DataOutput s]
    (pack-stream (double r) s))

  String
  (pack-stream
    [str ^java.io.DataOutput s]
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

  Character
  (pack-stream [c ^java.io.DataOutput s] (pack-stream (str c) s))

  clojure.lang.Keyword
  (pack-stream [kw ^java.io.DataOutput s] (pack-stream (name kw) s))

  clojure.lang.Symbol
  (pack-stream [sym ^java.io.DataOutput s] (pack-stream (name sym) s))

  Extension
  (pack-stream
    [e ^java.io.DataOutput s]
    (let [type (:type e)
          ^bytes data (:data e)
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
  (pack-stream [seq ^java.io.DataOutput s]
    (cond-let [len (count seq)]
              (<= len 0xf)
              (do (.writeByte s (bit-or 2r10010000 len)) (pack-coll seq s))

              (<= len 0xffff)
              (do (.writeByte s 0xdc) (.writeShort s len) (pack-coll seq s))

              (<= len 0xffffffff)
              (do (.writeByte s 0xdd) (.writeInt s len) (pack-coll seq s))))

  clojure.lang.IPersistentMap
  (pack-stream [map ^java.io.DataOutput s]
    (cond-let [len (count map)
               pairs (interleave (keys map) (vals map))]
              (<= len 0xf)
              (do (.writeByte s (bit-or 2r10000000 len)) (pack-coll pairs s))

              (<= len 0xffff)
              (do (.writeByte s 0xde) (.writeShort s len) (pack-coll pairs s))

              (<= len 0xffffffff)
              (do (.writeByte s 0xdf) (.writeInt s len) (pack-coll pairs s))))

  clojure.lang.IPersistentSet
  (pack-stream [set ^java.io.DataOutput s] (pack-stream (sequence set) s)))

;; TODO: friendlier error message?
;; Current:
;; java.lang.IllegalArgumentException: No implementation of method:
;; :pack-stream of protocol: #'msgpack.core/Packable found for class:
;; java.math.BigInteger

; Note: the extensions below are not in extend-protocol above because of
; a Clojure bug. See http://dev.clojure.org/jira/browse/CLJ-1381

; Array of java.lang.Byte (boxed)
(extend-type (class (java.lang.reflect.Array/newInstance Byte 0))
  Packable
  (pack-stream [bytes ^java.io.DataOutput s] (pack-bytes bytes s)))

; Array of primitive bytes (un-boxed)
(extend-type (Class/forName "[B")
  Packable
  (pack-stream [bytes ^java.io.DataOutput s] (pack-bytes bytes s)))

(defn pack [obj]
  (let [output-stream (ByteArrayOutputStream.)
        data-output (DataOutputStream. output-stream)]
    (do
      (pack-stream obj data-output)
      (.toByteArray output-stream))))

(defn- read-uint8
  [^java.io.DataInput data-input]
  (.readUnsignedByte data-input))

(defn- read-uint16
  [^java.io.DataInput data-input]
  (.readUnsignedShort data-input))

(defn- read-uint32
  [^java.io.DataInput data-input]
  (bit-and 0xffffffff (.readInt data-input)))

(defn- read-uint64
  [^java.io.DataInput data-input]
  (let [n (.readLong data-input)]
    (if (<= 0 n Long/MAX_VALUE)
      n
      (.and (biginteger n) (biginteger 0xffffffffffffffff)))))

(defn- read-bytes
  [n ^java.io.DataInput data-input]
  (let [bytes (byte-array n)]
    (do
      (.readFully data-input bytes)
      bytes)))

(defn- unpack-extension [n ^java.io.DataInput data-input]
  (->Extension (.readByte data-input) (read-bytes n data-input)))

(declare unpack-stream)

(defn- unpack-n [n ^java.io.DataInput data-input]
  (doall (for [_ (range n)] (unpack-stream data-input))))

(defn- unpack-map [n ^java.io.DataInput data-input]
  (apply hash-map (unpack-n (* 2 n) data-input)))

(defn unpack-stream [^java.io.DataInput data-input]
  (cond-let [byte (.readUnsignedByte data-input)]
            ; nil format family
            (= byte 0xc0) nil

            ; bool format family
            (= byte 0xc2) false
            (= byte 0xc3) true

            ; int format family
            (= (bit-and 2r11100000 byte) 2r11100000)
            (unchecked-byte byte)

            (= (bit-and 2r10000000 byte) 0)
            (unchecked-byte byte)

            (= byte 0xcc) (read-uint8 data-input)
            (= byte 0xcd) (read-uint16 data-input)
            (= byte 0xce) (read-uint32 data-input)
            (= byte 0xcf) (read-uint64 data-input)
            (= byte 0xd0) (.readByte data-input)
            (= byte 0xd1) (.readShort data-input)
            (= byte 0xd2) (.readInt data-input)
            (= byte 0xd3) (.readLong data-input)

            ; float format family
            (= byte 0xca) (.readFloat data-input)
            (= byte 0xcb) (.readDouble data-input)

            ; str format family
            (= (bit-and 2r11100000 byte) 2r10100000)
            (let [n (bit-and 2r11111 byte)]
              (String. ^bytes (read-bytes n data-input)))

            (= byte 0xd9)
            (String. ^bytes (read-bytes (read-uint8 data-input) data-input))

            (= byte 0xda)
            (String. ^bytes (read-bytes (read-uint16 data-input) data-input))

            (= byte 0xdb)
            (String. ^bytes (read-bytes (read-uint32 data-input) data-input))

            ; bin format family
            (= byte 0xc4)
            (read-bytes (read-uint8 data-input) data-input)

            (= byte 0xc5)
            (read-bytes (read-uint16 data-input) data-input)

            (= byte 0xc6)
            (read-bytes (read-uint32 data-input) data-input)

            ; ext format family
            (= byte 0xd4) (unpack-extension 1 data-input)
            (= byte 0xd5) (unpack-extension 2 data-input)
            (= byte 0xd6) (unpack-extension 4 data-input)
            (= byte 0xd7) (unpack-extension 8 data-input)
            (= byte 0xd8) (unpack-extension 16 data-input)

            (= byte 0xc7)
            (unpack-extension (read-uint8 data-input) data-input)

            (= byte 0xc8)
            (unpack-extension (read-uint16 data-input) data-input)

            (= byte 0xc9)
            (unpack-extension (read-uint32 data-input) data-input)

            ; array format family
            (= (bit-and 2r11110000 byte) 2r10010000)
            (unpack-n (bit-and 2r1111 byte) data-input)

            (= byte 0xdc)
            (unpack-n (read-uint16 data-input) data-input)

            (= byte 0xdd)
            (unpack-n (read-uint32 data-input) data-input)

            ; map format family
            (= (bit-and 2r11110000 byte) 2r10000000)
            (unpack-map (bit-and 2r1111 byte) data-input)

            (= byte 0xde)
            (unpack-map (read-uint16 data-input) data-input)

            (= byte 0xdf)
            (unpack-map (read-uint32 data-input) data-input)))

(defn unpack
  "Unpack bytes as MessagePack object."
  [bytes]
  (-> bytes
      byte-array
      ByteArrayInputStream.
      DataInputStream.
      unpack-stream))
