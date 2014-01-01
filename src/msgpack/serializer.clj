(ns msgpack.serializer
  (:require [msgpack.utils :refer :all]))

(defmulti serialize class)

(defn- with-header
  "Prepend a header byte to body."
  [header body]
  (cons (unsigned-byte header) body))

(defn- concat-serialize
  "Recursively serialize a sequence of items, then concatenate the result."
  [coll]
  (apply concat (map serialize coll)))

(defmethod serialize nil
  [_] (unsigned-bytes [0xc0]))

(defmethod serialize Boolean
  [bool]
  (if bool (unsigned-bytes [0xc3])
    (unsigned-bytes [0xc2])))

(derive Byte ::int)
(derive Short ::int)
(derive Integer ::int)
(derive Long ::int)
(defmethod serialize ::int
  [x]
  (cond
    (<= 0 x 127) (get-byte-bytes x)
    (<= -32 x -1) (get-byte-bytes x)
    (<= 0 x 0xff) (with-header 0xcc (get-byte-bytes x))
    (<= 0 x 0xffff) (with-header 0xcd (get-short-bytes x))
    (<= 0 x 0xffffffff) (with-header 0xce (get-int-bytes x))
    (<= 0 x 0x7fffffffffffffff) (with-header 0xcf (get-long-bytes x))
    (<= -0x80 x -1) (with-header 0xd0 (get-byte-bytes x))
    (<= -0x8000 x -1) (with-header 0xd1 (get-short-bytes x))
    (<= -0x80000000 x -1) (with-header 0xd2 (get-int-bytes x))
    (<= -0x8000000000000000 x -1) (with-header 0xd3 (get-long-bytes x))))

(defmethod serialize clojure.lang.BigInt
  [x]
  (if (<= 0x8000000000000000 x 0xffffffffffffffff)
    ; Extracts meaningful bits and drops sign.
    (with-header 0xcf (get-long-bytes (.longValue x)))
    (serialize (.longValue x))))

(defmethod serialize Float [n]
  (with-header 0xca (get-float-bytes n)))

(defmethod serialize Double [n]
  (with-header 0xcb (get-double-bytes n)))

(defmethod serialize String [s]
  (let [sbytes (seq (.getBytes s))
        len (count sbytes)]
    (cond
      (<= len 0x1f)
        (with-header (bit-or 2r10100000 len) sbytes)
      (<= len 0xff)
        (with-header 0xd9 (concat (get-byte-bytes len) sbytes))
      (<= len 0xffff)
        (with-header 0xda (concat (get-short-bytes len) sbytes))
      (<= len 0xffffffff)
        (with-header 0xdb (concat (get-int-bytes len) sbytes)))))

(derive (class (java.lang.reflect.Array/newInstance Byte 0)) ::byte-array)
(derive (class (byte-array nil)) ::byte-array)
(defmethod serialize ::byte-array
  [body]
  (let [len (count body)]
    (cond
      (<= len 0xff)
        (with-header 0xc4 (concat (get-byte-bytes len) body))
      (<= len 0xffff)
        (with-header 0xc5 (concat (get-short-bytes len) body))
      (<= len 0xffffffff)
        (with-header 0xc6 (concat (get-int-bytes len) body)))))

(derive clojure.lang.Sequential ::array)
(defmethod serialize ::array
  [coll]
  (let [len (count coll)
        body (concat-serialize coll)]
    (cond
      (<= len 0xf)
        (with-header (bit-or 2r10010000 len) body)
      (<= len 0xffff)
        (with-header 0xdc (concat (get-short-bytes len) body))
      (<= len 0xffffffff)
        (with-header 0xdd (concat (get-int-bytes len) body)))))

(derive clojure.lang.IPersistentMap ::map)
(defmethod serialize ::map
  [coll]
  (let [len (count coll)
        body (concat-serialize (interleave (keys coll) (vals coll)))]
    (cond
      (<= len 0xf)
        (with-header (bit-or 2r10000000 len) body)
      (<= len 0xffff)
        (with-header 0xde (concat (get-short-bytes len) body))
      (<= len 0xffffffff)
        (with-header 0xdf (concat (get-int-bytes len) body)))))
