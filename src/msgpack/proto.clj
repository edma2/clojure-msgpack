(ns msgpack.proto)

(defprotocol Extension
  "MessagePack allows applications to define application-specific types using
  the Extended type. Extended type consists of an integer and a byte array
  where the integer represents a kind of types and the byte array represents
  data."
  (ext-type [this])
  (ext-data [this]))
