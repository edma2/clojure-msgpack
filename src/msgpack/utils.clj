(ns msgpack.utils
  (:import java.io.ByteArrayOutputStream)
  (:import java.io.DataOutputStream))

;; Functions for converting unsigned integer values to signed values with
;; equivalent bit patterns.
;; Example:
;;   value is 0x80000000 which exceeds a 32-bit signed int
;;   bit-equivalent signed value is -0x80000000
(defn as-signed-byte [x]
  (byte (if (> x 0x7f) (- x 0x100) x)))

(defn as-signed-short [x]
  (short (if (> x 0x7fff) (- x 0x10000) x)))

(defn as-signed-int [x]
  (int (if (> x 0x7fffffff) (- x 0x100000000) x)))

(defn as-signed-long [x]
  (long (if (> x 0x7fffffffffffffff) (- x 0x10000000000000000) x)))

(defn byte-literal [x] (as-signed-byte x))
(defn byte-literals [xs] (map byte-literal xs))

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
