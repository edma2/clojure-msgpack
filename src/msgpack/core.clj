(ns msgpack.core
  (:import java.io.DataOutputStream
           java.io.ByteArrayOutputStream
           java.io.DataInputStream
           java.io.ByteArrayInputStream
           java.nio.charset.Charset))

(def ^:private ^Charset
  msgpack-charset
  (Charset/forName "UTF-8"))

(declare pack unpack unpack-stream)

(defprotocol Packable
  "Objects that can be serialized as MessagePack types"
  (packable-pack [this data-output opts]))

(defn pack-stream
  ([this data-output] (packable-pack this data-output nil))
  ([this data-output opts] (packable-pack this data-output opts)))

;; MessagePack allows applications to define application-specific types using
;; the Extended type. Extended type consists of an integer and a byte array
;; where the integer represents a kind of types and the byte array represents
;; data.
(defrecord Ext [type data])

(defmacro cond-let [bindings & clauses]
  `(let ~bindings (cond ~@clauses)))

(defn- pack-raw
  [^bytes bytes ^java.io.DataOutput s]
  (cond-let [len (count bytes)]
            (<= len 0x1f)
            (do (.writeByte s (bit-or 2r10100000 len)) (.write s bytes))

            (<= len 0xffff)
            (do (.writeByte s 0xda) (.writeShort s len) (.write s bytes))

            (<= len 0xffffffff)
            (do (.writeByte s 0xdb) (.writeInt s len) (.write s bytes))))

(defn- pack-str
  [^bytes bytes ^java.io.DataOutput s]
  (cond-let [len (count bytes)]
            (<= len 0x1f)
            (do (.writeByte s (bit-or 2r10100000 len)) (.write s bytes))

            (<= len 0xff)
            (do (.writeByte s 0xd9) (.writeByte s len) (.write s bytes))

            (<= len 0xffff)
            (do (.writeByte s 0xda) (.writeShort s len) (.write s bytes))

            (<= len 0xffffffff)
            (do (.writeByte s 0xdb) (.writeInt s len) (.write s bytes))))

(defn- pack-bytes
  [^bytes bytes ^java.io.DataOutput s]
  (cond-let [len (count bytes)]
            (<= len 0xff)
            (do (.writeByte s 0xc4) (.writeByte s len) (.write s bytes))

            (<= len 0xffff)
            (do (.writeByte s 0xc5) (.writeShort s len) (.write s bytes))

            (<= len 0xffffffff)
            (do (.writeByte s 0xc6) (.writeInt s len) (.write s bytes))))

(defn- pack-int
  "Pack integer using the most compact representation"
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
    (<= -0x8000000000000000 n -1) (do (.writeByte s 0xd3) (.writeLong s n))
    :else (throw (IllegalArgumentException. (str "Integer value out of bounds: " n)))))

(defn- pack-coll
  [coll ^java.io.DataOutput s opts]
  (doseq [item coll] (pack-stream item s opts)))

(extend-protocol Packable
  nil
  (packable-pack
    [_ ^java.io.DataOutput s _]
    (.writeByte s 0xc0))

  java.lang.Boolean
  (packable-pack
    [bool ^java.io.DataOutput s _]
    (if bool
      (.writeByte s 0xc3)
      (.writeByte s 0xc2)))

  java.lang.Byte
  (packable-pack [n ^java.io.DataOutput s _] (pack-int n s))

  java.lang.Short
  (packable-pack [n ^java.io.DataOutput s _] (pack-int n s))

  java.lang.Integer
  (packable-pack [n ^java.io.DataOutput s _] (pack-int n s))

  java.lang.Long
  (packable-pack [n ^java.io.DataOutput s _] (pack-int n s))

  java.math.BigInteger
  (packable-pack [n ^java.io.DataOutput s _] (pack-int n s))

  clojure.lang.BigInt
  (packable-pack [n ^java.io.DataOutput s _] (pack-int n s))

  java.lang.Float
  (packable-pack [f ^java.io.DataOutput s _]
    (do (.writeByte s 0xca) (.writeFloat s f)))

  java.lang.Double
  (packable-pack [d ^java.io.DataOutput s _]
    (do (.writeByte s 0xcb) (.writeDouble s d)))

  java.math.BigDecimal
  (packable-pack [d ^java.io.DataOutput s opts]
    (packable-pack (.doubleValue d) s opts))

  java.lang.String
  (packable-pack
    [str ^java.io.DataOutput s {:keys [raw]}]
    (let [bytes (.getBytes ^String str msgpack-charset)]
      (if raw
        (pack-raw bytes s)
        (pack-str bytes s))))

  Ext
  (packable-pack
    [e ^java.io.DataOutput s _]
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
  (packable-pack [seq ^java.io.DataOutput s opts]
    (cond-let [len (count seq)]
              (<= len 0xf)
              (do (.writeByte s (bit-or 2r10010000 len)) (pack-coll seq s opts))

              (<= len 0xffff)
              (do (.writeByte s 0xdc) (.writeShort s len) (pack-coll seq s opts))

              (<= len 0xffffffff)
              (do (.writeByte s 0xdd) (.writeInt s len) (pack-coll seq s opts))))

  clojure.lang.IPersistentMap
  (packable-pack [map ^java.io.DataOutput s opts]
    (cond-let [len (count map)
               pairs (interleave (keys map) (vals map))]
              (<= len 0xf)
              (do (.writeByte s (bit-or 2r10000000 len)) (pack-coll pairs s opts))

              (<= len 0xffff)
              (do (.writeByte s 0xde) (.writeShort s len) (pack-coll pairs s opts))

              (<= len 0xffffffff)
              (do (.writeByte s 0xdf) (.writeInt s len) (pack-coll pairs s opts)))))

; Note: the extensions below are not in extend-protocol above because of
; a Clojure bug. See http://dev.clojure.org/jira/browse/CLJ-1381

; Array of java.lang.Byte (boxed)
(extend (class (java.lang.reflect.Array/newInstance Byte 0))
  Packable
  {:packable-pack
   (fn [bytes ^java.io.DataOutput s {:keys [raw]}]
     (if raw
       (pack-raw bytes s)
       (pack-bytes bytes s)))})

(extend (Class/forName "[B")
  Packable
  {:packable-pack
   (fn [bytes ^java.io.DataOutput s {:keys [raw]}]
     (if raw
       (pack-raw bytes s)
       (pack-bytes bytes s)))})

(defn pack
  ([obj] (pack obj nil))
  ([obj opts]
   (let [output-stream (ByteArrayOutputStream.)
         data-output (DataOutputStream. output-stream)]
     (do
       (pack-stream obj data-output opts)
       (.toByteArray output-stream)))))

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

(defn- read-str
  [n ^java.io.DataInput data-input {:keys [raw]}]
  (let [bytes (read-bytes n data-input)]
    (if raw bytes
        (String. ^bytes bytes msgpack-charset))))

(defmulti refine-ext
  "Refine Extended type to an application-specific type."
  :type)

(defmethod refine-ext :default [ext] ext)

(defn- unpack-ext [n ^java.io.DataInput data-input]
  (refine-ext
   (->Ext (.readByte data-input) (read-bytes n data-input))))

(defn- unpack-n [n ^java.io.DataInput data-input opts]
  (doall (for [_ (range n)] (unpack-stream data-input opts))))

(defn- unpack-map [n ^java.io.DataInput data-input opts]
  (apply hash-map (unpack-n (* 2 n) data-input opts)))

(defn unpack-stream
  ([^java.io.DataInput data-input] (unpack-stream data-input nil))
  ([^java.io.DataInput data-input opts]
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
               (read-str n data-input opts))

             (= byte 0xd9)
             (read-str (read-uint8 data-input) data-input opts)

             (= byte 0xda)
             (read-str (read-uint16 data-input) data-input opts)

             (= byte 0xdb)
             (read-str (read-uint32 data-input) data-input opts)

             ; bin format family
             (= byte 0xc4)
             (read-bytes (read-uint8 data-input) data-input)

             (= byte 0xc5)
             (read-bytes (read-uint16 data-input) data-input)

             (= byte 0xc6)
             (read-bytes (read-uint32 data-input) data-input)

             ; ext format family
             (= byte 0xd4) (unpack-ext 1 data-input)
             (= byte 0xd5) (unpack-ext 2 data-input)
             (= byte 0xd6) (unpack-ext 4 data-input)
             (= byte 0xd7) (unpack-ext 8 data-input)
             (= byte 0xd8) (unpack-ext 16 data-input)

             (= byte 0xc7)
             (unpack-ext (read-uint8 data-input) data-input)

             (= byte 0xc8)
             (unpack-ext (read-uint16 data-input) data-input)

             (= byte 0xc9)
             (unpack-ext (read-uint32 data-input) data-input)

             ; array format family
             (= (bit-and 2r11110000 byte) 2r10010000)
             (unpack-n (bit-and 2r1111 byte) data-input opts)

             (= byte 0xdc)
             (unpack-n (read-uint16 data-input) data-input opts)

             (= byte 0xdd)
             (unpack-n (read-uint32 data-input) data-input opts)

             ; map format family
             (= (bit-and 2r11110000 byte) 2r10000000)
             (unpack-map (bit-and 2r1111 byte) data-input opts)

             (= byte 0xde)
             (unpack-map (read-uint16 data-input) data-input opts)

             (= byte 0xdf)
             (unpack-map (read-uint32 data-input) data-input opts))))

(defn- to-byte-array
  [bytes]
  (if (instance? (Class/forName "[B") bytes)
    bytes
    (byte-array bytes)))

(defn unpack
  "Unpack bytes as MessagePack object."
  ([bytes] (unpack bytes nil))
  ([bytes opts]
   (let [data-input (-> bytes
                        to-byte-array
                        ByteArrayInputStream.
                        DataInputStream.)]
     (unpack-stream data-input opts))))
