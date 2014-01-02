(ns msgpack.core
  (:require [msgpack.serializer :as serializer]
            [msgpack.deserializer :as deserializer]))

(defn serialize
  "Serialize an object as a sequence of bytes in MessagePack format."
  [thing]
  (serializer/serialize thing))

(defn deserialize
  "Deserialize a sequence of bytes in MessagePack format to an object."
  [bytes]
  (deserializer/deserialize bytes))
