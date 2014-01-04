(ns msgpack.util
  (:import java.io.ByteArrayOutputStream
           java.io.DataOutputStream))

(defn B
  "Treat n as if it were an unsigned byte literal. If n is greater
  than the maximum value of a signed byte (127), convert it to a
  negative byte value with the same pattern of bits."
  [n]
  (byte (if (> n 0x7f) (- n 0x100) n)))

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
