(ns msgpack.serializer
  (:require [msgpack.utils :refer :all]))

(defmulti serialize class)

(defmethod serialize nil
  [_] (byte-literals [0xc0]))

(defmethod serialize Boolean
  [bool]
  (if bool (byte-literals [0xc3])
    (byte-literals [0xc2])))

(defn- int-bytes
  [x n]
  (if (= 0 n) []
  (conj (int-bytes (bit-shift-right x 8) (dec n))
        (byte-literal (bit-and 0xff x)))))

(defmethod serialize Long
  [x]
  (cond
    (<= 0 x 127) (byte-literals [x])
    (<= -32 x -1) (byte-literals [x])
    (<= 0 x 0xff) (byte-literals [0xcc x])
    (<= 0 x 0xffff) (cons (byte-literal 0xcd) (int-bytes x 2))
    (<= 0 x 0xffffffff) (cons (byte-literal 0xce) (int-bytes x 4))
    (<= 0 x 0x7fffffffffffffff) (cons (byte-literal 0xcf) (int-bytes x 8))
    (<= -0x80 x 0x7f) (byte-literals [0xd0 x])
    (<= -0x8000 x 0x7fff) (cons (byte-literal 0xd1) (int-bytes x 2))
    (<= -0x80000000 x 0x7fffffff) (cons (byte-literal 0xd2) (int-bytes x 4))
    (<= -0x8000000000000000 x 0x7fffffffffffffff)
      (cons (byte-literal 0xd3) (int-bytes x 8))))

; Long can handle up to 2^63-1 integers. MessagePack max is 2^64-1.
(defmethod serialize clojure.lang.BigInt
  [x]
  (cond
    ; Get lowest 64 bits, ignore sign (we know it is positive).
    (<= 0x8000000000000000 x 0xffffffffffffffff)
      (cons (byte-literal 0xcf) (int-bytes (.longValue x) 8))
    ; In case they use small values with explicit BigInt type, e.g. 314N
    :else (serialize (.longValue x))))

; Cast everything else to Long
(defmethod serialize Integer [x] (serialize (long x)))
(defmethod serialize Short [x] (serialize (long x)))
(defmethod serialize Byte [x] (serialize (long x)))
