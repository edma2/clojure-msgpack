(ns msgpack.core-check
  (:require [msgpack.core :as msg]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(defspec round-trip 20
  (prop/for-all [v (gen/map gen/any gen/any)]
                (= (msg/unpack (msg/pack v)) v)))
