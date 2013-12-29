(ns msgpack.serializer
  (:require [msgpack.utils :refer :all]))

(defmulti serialize class)

(defmethod serialize nil
  [_] (byte-literals [0xc0]))

(defmethod serialize Boolean
  [bool]
  (if bool (byte-literals [0xc3])
    (byte-literals [0xc2])))

(defmethod serialize Long
  [x]
  (cond
    (<= 0 x 127) (byte-literals [x])
    (<= -32 x -1) (byte-literals [x])
    (<= 0 x 0xff) (byte-literals [0xcc x])))
