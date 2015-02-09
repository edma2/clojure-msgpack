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

  Long
  (unsigned [n]
    (if (neg? n)
      (bigint (.and (biginteger n) (biginteger 0xffffffffffffffff)))
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

(defn byte->bytes [n] (->bytes #(.writeByte ^java.io.DataOutputStream %1 %2) n))
(defn short->bytes [n] (->bytes #(.writeShort ^java.io.DataOutputStream %1 %2) n))
(defn int->bytes [n] (->bytes #(.writeInt ^java.io.DataOutputStream %1 %2) n))
(defn long->bytes [n] (->bytes #(.writeLong ^java.io.DataOutputStream %1 %2) n))
(defn float->bytes [x] (->bytes #(.writeFloat ^java.io.DataOutputStream %1 %2) x))
(defn double->bytes [x] (->bytes #(.writeDouble ^java.io.DataOutputStream %1 %2) x))

(defn byte-stream [bytes]
  (DataInputStream. (ByteArrayInputStream. (byte-array bytes))))

;; DataInputStream
(defn next-byte [stream] (.readByte ^java.io.DataInputStream stream))
(defn next-short [stream] (.readShort ^java.io.DataInputStream stream))
(defn next-int [stream] (.readInt ^java.io.DataInputStream stream))
(defn next-long [stream] (.readLong ^java.io.DataInputStream stream))
(defn next-float [stream] (.readFloat ^java.io.DataInputStream stream))
(defn next-double [stream] (.readDouble ^java.io.DataInputStream stream))
(defn next-bytes [n stream]
  (if (zero? n) (byte-array 0)
      (let [bytes (byte-array n)
            bytes-read (.read ^java.io.DataInputStream stream bytes)]
        (assert (= n bytes-read))
        bytes)))

(defn next-string [n stream] (String. ^bytes (next-bytes n stream)))
