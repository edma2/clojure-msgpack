(ns msgpack.macros
  (require [msgpack.core :refer :all]))

(defmacro defext
  "Treat an existing class as a MessagePack extended type.
  As a side-effect, the class will extend the Packable protocol.
  Provide a type (non-negative signed integer) and a function that
  converts the class to a sequence of bytes.

  (defrecord Employee [name])
  (defext Employee 1 #(.getBytes (:name %)))

  expands into:

  (extend-protocol Packable
    Employee
    (pack-stream [obj stream]
      (pack-stream (->Extended 1 (.getBytes (:name obj))) stream)))

  and this will work:
  (pack (Employee. name))"
  [class type f]
  `(let [type# ~type]
     (assert (<= 0 type# 127)
             "[-1, -128]: reserved for future pre-defined extensions.")
     (extend-protocol Packable
       ~class
       (pack-stream [obj# stream#] (pack-stream (->Extended type# (~f obj#)) stream#)))))
