(ns msgpack.utils
  (:import java.io.ByteArrayOutputStream
           java.io.DataOutputStream))

(defn ubyte [n]
  (byte (if (> n 0x7f) (- n 0x100) n)))

(defn ubytes [seq]
  (map ubyte seq))

(defn ubyte-array [seq]
  (byte-array (ubytes seq)))

(defn- get-bytes
  "Convert a Java primitive to its byte representation."
  [write n]
  (let [output-stream (ByteArrayOutputStream.)
        data-output (DataOutputStream. output-stream)]
    (write data-output n)
    (.toByteArray output-stream)))

(defn get-byte-bytes [n] (get-bytes #(.writeByte %1 %2) n))
(defn get-short-bytes [n] (get-bytes #(.writeShort %1 %2) n))
(defn get-int-bytes [n] (get-bytes #(.writeInt %1 %2) n))
(defn get-long-bytes [n] (get-bytes #(.writeLong %1 %2) n))
(defn get-float-bytes [n] (get-bytes #(.writeFloat %1 %2) n))
(defn get-double-bytes [n] (get-bytes #(.writeDouble %1 %2) n))
