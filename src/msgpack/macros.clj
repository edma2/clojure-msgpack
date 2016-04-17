(ns msgpack.macros
  "Macros for extending MessagePack with Extended types.
  See msgpack.clojure-extensions for examples."
  (:require [msgpack.core :refer :all]))

(defmacro extend-msgpack
  [class type pack-args pack unpack-args unpack]
  `(let [type# ~type]
     (assert (<= 0 type# 127)
             "[-1, -128]: reserved for future pre-defined extensions.")
     (do
       (extend-protocol Packable ~class
                        (packable-pack [~@pack-args ^java.io.DataOutput s# opts#]
                          (packable-pack (->Ext type# ~pack) s# opts#)))
       (defmethod refine-ext type# [ext#]
         (let [~@unpack-args (:data ext#)]
           ~unpack)))))
