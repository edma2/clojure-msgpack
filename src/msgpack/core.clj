(ns msgpack.core
  (:require [msgpack.serializer :as serializer]))

(defn serialize
  "Serialize an object as a sequence of bytes in MessagePack format."
  [thing]
  (serializer/serialize thing))
