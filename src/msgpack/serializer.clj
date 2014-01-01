(ns msgpack.serializer
  (:require [msgpack.utils :refer :all]))

(defmulti serialize class)

(defmethod serialize nil
  [_] (unsigned-bytes [0xc0]))

(defmethod serialize Boolean
  [bool]
  (if bool (unsigned-bytes [0xc3])
    (unsigned-bytes [0xc2])))

(defn- with-header [h bseq]
  (cons (unsigned-byte h) bseq))

(defmethod serialize Long
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

; Long can handle up to 2^63-1 integers. MessagePack max is 2^64-1.
; Clojure coerces values greater than 2^63-1 to BigInts.
(defmethod serialize clojure.lang.BigInt
  [x]
  (if (<= 0x8000000000000000 x 0xffffffffffffffff)
    (with-header 0xcf (get-long-bytes (.longValue x)))
    ; In case they use small values with explicit BigInt type, e.g. 314N
    (serialize (.longValue x))))

; Cast all other integral types to Long
(defmethod serialize Integer [x] (serialize (long x)))
(defmethod serialize Short [x] (serialize (long x)))
(defmethod serialize Byte [x] (serialize (long x)))

(defmethod serialize Double [n]
  (with-header 0xcb (get-double-bytes n)))

(defmethod serialize Float [n]
  (with-header 0xca (get-float-bytes n)))

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
  [bytes]
  (let [len (count bytes)]
    (cond
      (<= len 0xff)
        (with-header 0xc4 (concat (get-byte-bytes len) bytes))
      (<= len 0xffff)
        (with-header 0xc5 (concat (get-short-bytes len) bytes))
      (<= len 0xffffffff)
        (with-header 0xc6 (concat (get-int-bytes len) bytes)))))

; Recursively serialize a sequence of items, then concatenate the result.
(defn- serialize-all
  [seq]
  (apply concat (map serialize seq)))

; TODO: what kind of dispatch type?
(defmethod serialize clojure.lang.Sequential
  [seq]
  (let [len (count seq)
        body (serialize-all seq)]
    (cond
      (<= len 0xf)
        (with-header (bit-or 2r10010000 len) body)
      (<= len 0xffff)
        (with-header 0xdc (concat (get-short-bytes len) body))
      (<= len 0xffffffff)
        (with-header 0xdd (concat (get-int-bytes len) body)))))

(defmethod serialize clojure.lang.IPersistentMap
  [map]
  (let [len (count map)
        body (serialize-all (interleave (keys map) (vals map)))]
    (cond
      (<= len 0xf)
        (with-header (bit-or 2r10000000 len) body)
      (<= len 0xffff)
        (with-header 0xde (concat (get-short-bytes len) body))
      (<= len 0xffffffff)
        (with-header 0xdf (concat (get-int-bytes len) body)))))
