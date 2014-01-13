(ns msgpack.io
  (:import java.io.ByteArrayOutputStream
           java.io.DataOutputStream
           java.io.ByteArrayInputStream
           java.io.DataInputStream))

(defprotocol Unsignable
  "A number whose bit pattern can be interpreted as an unsigned value
  instead of two's complement (signed)."
  (unsigned [n]))

(extend-protocol Unsignable
  Byte (unsigned [n] (bit-and 0xff n))
  Short (unsigned [n] (bit-and 0xffff n))
  Integer (unsigned [n] (bit-and 0xffffffff n))

  Long ; Might return a BigInt
  (unsigned [n]
    (if (neg? n)
      (bigint (.and (biginteger n) (biginteger 0xffffffffffffffff)))
      ;; bigint stuff
      n)))

(defn ubyte
  "Treat n as if it were an unsigned byte literal. If n is greater
  than the maximum value of a signed byte (127), convert it to a
  negative byte value with the same pattern of bits."
  [n]
  (byte (if (> n 0x7f) (- n 0x100) n)))

(defn ubytes [coll] (byte-array (map ubyte coll)))

(defn- ->bytes
  "Convert a Java primitive to its byte representation."
  [write v]
  (let [output-stream (ByteArrayOutputStream.)
        data-output (DataOutputStream. output-stream)]
    (write data-output v)
    (.toByteArray output-stream)))

(defn byte->bytes [n] (->bytes #(.writeByte %1 %2) n))
(defn short->bytes [n] (->bytes #(.writeShort %1 %2) n))
(defn int->bytes [n] (->bytes #(.writeInt %1 %2) n))
(defn long->bytes [n] (->bytes #(.writeLong %1 %2) n))
(defn float->bytes [x] (->bytes #(.writeFloat %1 %2) x))
(defn double->bytes [x] (->bytes #(.writeDouble %1 %2) x))

(defn byte-stream [bytes]
  (let [input-stream (ByteArrayInputStream. (byte-array bytes))
        data-input (DataInputStream. input-stream)]
    data-input))

;; DataInputStream
(defn next-byte [stream] (.readByte stream))
(defn next-short [stream] (.readShort stream))
(defn next-int [stream] (.readInt stream))
(defn next-long [stream] (.readLong stream))
(defn next-float [stream] (.readFloat stream))
(defn next-double [stream] (.readDouble stream))
(defn next-bytes [n stream]
  (let [bytes (byte-array n)]
    (assert (.read stream bytes))
    bytes))

(defn next-string [n stream] (String. (next-bytes n stream)))
