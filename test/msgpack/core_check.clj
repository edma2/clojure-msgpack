(ns msgpack.core-check
  (:require [msgpack.core :as msg]
            [msgpack.macros :refer [extend-msgpack]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

; gen/any can return UUIDs
(extend-msgpack
 java.util.UUID
 42
 [id] (msg/pack (str id))
 [bytes] (java.util.UUID/fromString (msg/unpack bytes)))

(defspec round-trip 40
  (prop/for-all [v gen/any]
                (= (msg/unpack (msg/pack v)) v)))
