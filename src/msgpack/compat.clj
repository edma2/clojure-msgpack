(ns msgpack.compat
  "Compatibility mode for old MessagePack spec."
  (:require [msgpack.core :as msg]))

(defn pack [obj]
  (binding [msg/*compatibility-mode* true]
    (msg/pack obj)))
