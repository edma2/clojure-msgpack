(ns msgpack.macros
  "Macros for extending MessagePack with Extended types.
  See msgpack.clojure-extensions for examples."
  (:require [msgpack.core :refer :all]))

(defmacro extend-msgpack
  [class type unpack-args unpack pack-args pack]
  `(let [type# ~type]
     (assert (<= 0 type# 127)
             "[-1, -128]: reserved for future pre-defined extensions.")
     (do
       (extend-protocol Packable ~class
                        (pack-stream [~@pack-args ^java.io.DataOutput s#]
                          (pack-stream (->Ext type# ~pack) s#)))
       (defmethod refine-ext type# [ext#]
         (let [~@unpack-args (:data ext#)]
           ~unpack)))))
