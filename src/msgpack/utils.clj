(ns msgpack.utils
  (:import java.io.ByteArrayOutputStream)
  (:import java.io.DataOutputStream))

(defn unsigned-byte [x]
  (byte (if (> x 0x7f) (- x 0x100) x)))

(defn unsigned-bytes [xs]
  (map unsigned-byte xs))

;; Functions for converting integer values to byte Seqs.
(defn- get-bytes
  [write x]
  (let [output-stream (new ByteArrayOutputStream)
        data-output (new DataOutputStream output-stream)]
    (write data-output x)
    (seq (.toByteArray output-stream))))

(defn get-byte-bytes [x] (get-bytes #(.writeByte %1 %2) x))
(defn get-short-bytes [x] (get-bytes #(.writeShort %1 %2) x))
(defn get-int-bytes [x] (get-bytes #(.writeInt %1 %2) x))
(defn get-long-bytes [x] (get-bytes #(.writeLong %1 %2) x))
