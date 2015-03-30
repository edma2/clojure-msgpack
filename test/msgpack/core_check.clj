(ns msgpack.core-check
  (:require [msgpack.core :refer [pack unpack]]
            [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(defmulti normalize class)
(defmethod normalize :default [v] v)

(defmethod normalize clojure.lang.Ratio [r]
  (let [d (double r)]
    (if (<= d Float/MAX_VALUE) (float d) d)))

(defmethod normalize (Class/forName "[B") [bytes] (seq bytes))
(defmethod normalize Character [c] (str c))
(defmethod normalize clojure.lang.Keyword [k] (name k))
(defmethod normalize clojure.lang.Symbol [s] (name s))

(defmethod normalize clojure.lang.Sequential [seq]
  (map normalize seq))

(defmethod normalize clojure.lang.IPersistentSet [set]
  (normalize (vec set)))

(defmethod normalize clojure.lang.IPersistentMap [m]
  (into {} (for [[k v] m] [(normalize k) (normalize v)])))

(defspec round-trip 20
  (prop/for-all [v (gen/vector gen/any)]
                (= (unpack (pack v))
                   (normalize v))))
