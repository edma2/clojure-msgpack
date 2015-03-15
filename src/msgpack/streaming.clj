(ns msgpack.streaming
  (:require [msgpack.io :refer :all])
  (:import java.io.DataOutputStream
           java.io.ByteArrayOutputStream))

(defprotocol Packable
  "A protocol for objects that can be serialized as a MessagePack type."
  (pack-stream [this output-stream]))

(defn- byte-literals
  [bytes]
  (map unchecked-byte bytes))

(extend-protocol Packable
  nil
  (pack-stream [_ output-stream]
    (.writeByte output-stream 0xc0))

  Boolean
  (pack-stream [bool output-stream]
    (if bool (.writeByte output-stream 0xc3)
      (.writeByte output-stream 0xc2))))

(defn pack [obj]
  (let [baos (ByteArrayOutputStream.)
        dos (DataOutputStream. baos)]
    (do
      (pack-stream obj dos)
      (seq (.toByteArray baos)))))
