(ns msgpack.core-check
  (:require [msgpack.core :as msg]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

; NaN is never equal to itself
(defn- not-nan [x]
  (not (Double/isNaN x)))

(defn- pack-and-unpack [x] (msg/unpack (msg/pack x)))

(defspec ints-round-trip 100
  (prop/for-all [x gen/int]
                (= (pack-and-unpack x) x)))

(defspec floats-round-trip 100
  (prop/for-all [x (gen/such-that not-nan gen/double)]
                (= (pack-and-unpack x) x)))

(defspec bytes-round-trip 100
  (prop/for-all [x gen/bytes]
                (let [bytes (pack-and-unpack x)]
                  (and (instance? (Class/forName "[B") bytes)
                       (= (seq bytes) (seq x))))))

(defspec strings-round-trip 100
  (prop/for-all [x gen/string]
                (= (pack-and-unpack x) x)))

(defspec vectors-round-trip 100
  (prop/for-all [x (gen/vector (gen/map gen/int gen/string))]
                (= (pack-and-unpack x) x)))
