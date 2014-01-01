(ns msgpack.serializer
  (:require [msgpack.utils :refer :all]
            [msgpack.proto :refer :all]))

(defmulti serialize class)

(defn- serialize-concat
  "Recursively serialize a sequence of items, then concatenate the result."
  [coll]
  (apply concat (map serialize coll)))

(defmethod serialize nil
  [_] (ubyte-array [0xc0]))

(defmethod serialize Boolean
  [bool]
  (if bool (ubyte-array [0xc3])
    (ubyte-array [0xc2])))

(derive Byte ::int)
(derive Short ::int)
(derive Integer ::int)
(derive Long ::int)
(defmethod serialize ::int
  [x]
  (cond
    (<= 0 x 127)                  (ubyte-array (get-byte-bytes x))
    (<= -32 x -1)                 (ubyte-array (get-byte-bytes x))
    (<= 0 x 0xff)                 (ubyte-array (cons 0xcc (get-byte-bytes x)))
    (<= 0 x 0xffff)               (ubyte-array (cons 0xcd (get-short-bytes x)))
    (<= 0 x 0xffffffff)           (ubyte-array (cons 0xce (get-int-bytes x)))
    (<= 0 x 0x7fffffffffffffff)   (ubyte-array (cons 0xcf (get-long-bytes x)))
    (<= -0x80 x -1)               (ubyte-array (cons 0xd0 (get-byte-bytes x)))
    (<= -0x8000 x -1)             (ubyte-array (cons 0xd1 (get-short-bytes x)))
    (<= -0x80000000 x -1)         (ubyte-array (cons 0xd2 (get-int-bytes x)))
    (<= -0x8000000000000000 x -1) (ubyte-array (cons 0xd3 (get-long-bytes x)))))

(defmethod serialize clojure.lang.BigInt
  [x]
  (if (<= 0x8000000000000000 x 0xffffffffffffffff)
    ;; Extracts meaningful bits and drops sign.
    (ubyte-array (cons 0xcf (get-long-bytes (.longValue x))))
    (serialize (.longValue x))))

(defmethod serialize Float [n]
  (ubyte-array (cons 0xca (get-float-bytes n))))

(defmethod serialize Double [n]
  (ubyte-array (cons 0xcb (get-double-bytes n))))

(defmethod serialize clojure.lang.Keyword [k]
  (serialize (name k)))

(defmethod serialize clojure.lang.Symbol [s]
  (serialize (name s)))

(defmethod serialize String [s]
  (let [data (.getBytes s)
        size (count data)]
    (cond
      (<= size 0x1f)       (ubyte-array (cons (bit-or 2r10100000 size) data))
      (<= size 0xff)       (ubyte-array (concat [0xd9] (get-byte-bytes size) data))
      (<= size 0xffff)     (ubyte-array (concat [0xda] (get-short-bytes size) data))
      (<= size 0xffffffff) (ubyte-array (concat [0xdb] (get-int-bytes size) data)))))

(derive (class (java.lang.reflect.Array/newInstance Byte 0)) ::byte-array)
(derive (class (byte-array nil)) ::byte-array)
(defmethod serialize ::byte-array
  [data]
  (let [size (count data)]
    (cond
      (<= size 0xff)       (ubyte-array (concat [0xc4] (get-byte-bytes size) data))
      (<= size 0xffff)     (ubyte-array (concat [0xc5] (get-short-bytes size) data))
      (<= size 0xffffffff) (ubyte-array (concat [0xc6] (get-int-bytes size) data)))))

(derive clojure.lang.Sequential ::array)
(defmethod serialize ::array
  [coll]
  (let [size (count coll)
        data (serialize-concat coll)]
    (cond
      (<= size 0xf)        (ubyte-array (cons (bit-or 2r10010000 size) data))
      (<= size 0xffff)     (ubyte-array (concat [0xdc] (get-short-bytes size) data))
      (<= size 0xffffffff) (ubyte-array (concat [0xdd] (get-int-bytes size) data)))))

(derive clojure.lang.IPersistentMap ::map)
(defmethod serialize ::map
  [coll]
  (let [size (count coll)
        data (serialize-concat (interleave (keys coll) (vals coll)))]
    (cond
      (<= size 0xf)        (ubyte-array (cons (bit-or 2r10000000 size) data))
      (<= size 0xffff)     (ubyte-array (concat [0xde] (get-short-bytes size) data))
      (<= size 0xffffffff) (ubyte-array (concat [0xdf] (get-int-bytes size) data)))))

(prefer-method serialize (:on-interface Extension) ::map)
(defmethod serialize (:on-interface Extension)
  [ext]
  (let [type (ext-type ext)
        data (ext-data ext)
        size (count data)]
    ;; Negative types are reserved for future use.
    (assert (<= 0 type 127))
    (cond
      (= size 1)           (ubyte-array (concat [0xd4 type] data))
      (= size 2)           (ubyte-array (concat [0xd5 type] data))
      (= size 4)           (ubyte-array (concat [0xd6 type] data))
      (= size 8)           (ubyte-array (concat [0xd7 type] data))
      (= size 16)          (ubyte-array (concat [0xd8 type] data))
      (<= size 0xff)       (ubyte-array (concat (cons 0xc7 (get-byte-bytes size)) (cons type data)))
      (<= size 0xffff)     (ubyte-array (concat (cons 0xc8 (get-short-bytes size)) (cons type data)))
      (<= size 0xffffffff) (ubyte-array (concat (cons 0xc9 (get-int-bytes size)) (cons type data))))))
