(ns msgpack.serializer
  (:require [msgpack.utils :refer :all]))

(defmulti serialize class)

(defmethod serialize nil
  [_] (byte-literals [0xc0]))

(defmethod serialize Boolean
  [bool]
  (if bool (byte-literals [0xc3])
    (byte-literals [0xc2])))

(defn- with-header [h bseq]
  (cons (byte-literal h) bseq))

(defmethod serialize Long
  [x]
  (cond
    (<= 0 x 127) (get-byte-bytes x)
    (<= -32 x -1) (get-byte-bytes x)
    (<= 0 x 0xff) (with-header 0xcc (get-byte-bytes x))
    (<= 0 x 0xffff) (with-header 0xcd (get-short-bytes x))
    (<= 0 x 0xffffffff) (with-header 0xce (get-int-bytes x))
    (<= 0 x 0x7fffffffffffffff) (with-header 0xcf (get-long-bytes x))
    (<= -0x80 x 0x7f) (with-header 0xd0 (get-byte-bytes x))
    (<= -0x8000 x 0x7fff) (with-header 0xd1 (get-short-bytes x))
    (<= -0x80000000 x 0x7fffffff) (with-header 0xd2 (get-int-bytes x))
    (<= -0x8000000000000000 x 0x7fffffffffffffff)
      (with-header 0xd3 (get-long-bytes x))))

; Long can handle up to 2^63-1 integers. MessagePack max is 2^64-1.
; Clojure coerces values greater than 2^63-1 to BigInts.
(defmethod serialize clojure.lang.BigInt
  [x]
  (if (<= 0x8000000000000000 x 0xffffffffffffffff)
    (with-header 0xcf (get-long-bytes (.longValue x)))
    ; In case they use small values with explicit BigInt type, e.g. 314N
    (serialize (.longValue x))))

; Cast everything else to Long
(defmethod serialize Integer [x] (serialize (long x)))
(defmethod serialize Short [x] (serialize (long x)))
(defmethod serialize Byte [x] (serialize (long x)))
