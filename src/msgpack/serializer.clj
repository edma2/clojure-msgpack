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
  (if (< x 128)
    (byte-literals [x])
    nil)) ; error
