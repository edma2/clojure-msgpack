(ns msgpack.serializer
  (:require [msgpack.utils :refer :all]))

(defmulti serialize class)

(defn- msgpack-bytes
  ([header data]
   (let [header (if (number? header)
                  [(unsigned-byte header)]
                  (unsigned-bytes header))]
   (byte-array (concat header (unsigned-bytes data)))))
  ([data] (msgpack-bytes nil data)))

(defn- serialize-concat
  "Recursively serialize a sequence of items, then concatenate the result."
  [coll]
  (apply concat (map serialize coll)))

(defmethod serialize nil
  [_] (msgpack-bytes [0xc0]))

(defmethod serialize Boolean
  [bool]
  (if bool (msgpack-bytes [0xc3])
    (msgpack-bytes [0xc2])))

(derive Byte ::int)
(derive Short ::int)
(derive Integer ::int)
(derive Long ::int)
(defmethod serialize ::int
  [x]
  (cond
    (<= 0 x 127) (msgpack-bytes (get-byte-bytes x))
    (<= -32 x -1) (msgpack-bytes (get-byte-bytes x))
    (<= 0 x 0xff) (msgpack-bytes 0xcc (get-byte-bytes x))
    (<= 0 x 0xffff) (msgpack-bytes 0xcd (get-short-bytes x))
    (<= 0 x 0xffffffff) (msgpack-bytes 0xce (get-int-bytes x))
    (<= 0 x 0x7fffffffffffffff) (msgpack-bytes 0xcf (get-long-bytes x))
    (<= -0x80 x -1) (msgpack-bytes 0xd0 (get-byte-bytes x))
    (<= -0x8000 x -1) (msgpack-bytes 0xd1 (get-short-bytes x))
    (<= -0x80000000 x -1) (msgpack-bytes 0xd2 (get-int-bytes x))
    (<= -0x8000000000000000 x -1) (msgpack-bytes 0xd3 (get-long-bytes x))))

(defmethod serialize clojure.lang.BigInt
  [x]
  (if (<= 0x8000000000000000 x 0xffffffffffffffff)
    ; Extracts meaningful bits and drops sign.
    (msgpack-bytes 0xcf (get-long-bytes (.longValue x)))
    (serialize (.longValue x))))

(defmethod serialize Float [n]
  (msgpack-bytes 0xca (get-float-bytes n)))

(defmethod serialize Double [n]
  (msgpack-bytes 0xcb (get-double-bytes n)))

(defmethod serialize String [s]
  (let [data (seq (.getBytes s))
        size (count data)]
    (cond
      (<= size 0x1f)
        (msgpack-bytes (bit-or 2r10100000 size) data)
      (<= size 0xff)
        (msgpack-bytes (cons 0xd9 (get-byte-bytes size)) data)
      (<= size 0xffff)
        (msgpack-bytes (cons 0xda (get-short-bytes size)) data)
      (<= size 0xffffffff)
        (msgpack-bytes (cons 0xdb (get-int-bytes size)) data))))

(derive (class (java.lang.reflect.Array/newInstance Byte 0)) ::byte-array)
(derive (class (byte-array nil)) ::byte-array)
(defmethod serialize ::byte-array
  [data]
  (let [size (count data)]
    (cond
      (<= size 0xff)
        (msgpack-bytes (cons 0xc4 (get-byte-bytes size)) data)
      (<= size 0xffff)
        (msgpack-bytes (cons 0xc5 (get-short-bytes size)) data)
      (<= size 0xffffffff)
        (msgpack-bytes (cons 0xc6 (get-int-bytes size)) data))))

(derive clojure.lang.Sequential ::array)
(defmethod serialize ::array
  [coll]
  (let [size (count coll)
        data (serialize-concat coll)]
    (cond
      (<= size 0xf)
        (msgpack-bytes (bit-or 2r10010000 size) data)
      (<= size 0xffff)
        (msgpack-bytes (cons 0xdc (get-short-bytes size)) data)
      (<= size 0xffffffff)
        (msgpack-bytes (cons 0xdd (get-int-bytes size)) data))))

(derive clojure.lang.IPersistentMap ::map)
(defmethod serialize ::map
  [coll]
  (let [size (count coll)
        data (serialize-concat (interleave (keys coll) (vals coll)))]
    (cond
      (<= size 0xf)
        (msgpack-bytes (bit-or 2r10000000 size) data)
      (<= size 0xffff)
        (msgpack-bytes (cons 0xde (get-short-bytes size)) data)
      (<= size 0xffffffff)
        (msgpack-bytes (cons 0xdf (get-int-bytes size)) data))))
