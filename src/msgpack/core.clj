(ns msgpack.core
  (:require '[msgpack.serializer :as serializer]))

(defn serialize [thing]
  (serializer/serialize thing))
