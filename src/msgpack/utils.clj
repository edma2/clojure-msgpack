(ns msgpack.utils
  (:import java.io.ByteArrayOutputStream)
  (:import java.io.DataOutputStream))

(defn ubyte [x]
  (byte (if (> x 0x7f) (- x 0x100) x)))

(defn ubytes [xs]
  (map ubyte xs))

(defn ubyte-array [xs]
  (byte-array (ubytes xs)))

(defn- get-bytes
  "Convert a Java primitive to its byte representation."
  [write x]
  (let [output-stream (new ByteArrayOutputStream)
        data-output (new DataOutputStream output-stream)]
    (write data-output x)
    (.toByteArray output-stream)))

(defn get-byte-bytes [x] (get-bytes #(.writeByte %1 %2) x))
(defn get-short-bytes [x] (get-bytes #(.writeShort %1 %2) x))
(defn get-int-bytes [x] (get-bytes #(.writeInt %1 %2) x))
(defn get-long-bytes [x] (get-bytes #(.writeLong %1 %2) x))
(defn get-float-bytes [n] (get-bytes #(.writeFloat %1 %2) n))
(defn get-double-bytes [n] (get-bytes #(.writeDouble %1 %2) n))
