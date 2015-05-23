(ns msgpack.clojure-extensions
  "Extended types for Clojure-specific types"
  (:require [msgpack.core :as msg]
            [msgpack.macros :refer [extend-msgpack]]))

(extend-msgpack
 clojure.lang.Keyword
 3
 [bytes] (keyword (msg/unpack bytes))
 [k] (msg/pack (name k)))

(extend-msgpack
 clojure.lang.Symbol
 4
 [bytes] (symbol (msg/unpack bytes))
 [s] (msg/pack (name s)))

(extend-msgpack
 java.lang.Character
 5
 [bytes] (first (char-array (msg/unpack bytes)))
 [c] (msg/pack (str c)))

(extend-msgpack
 clojure.lang.Ratio
 6
 [bytes] (let [seq (msg/unpack bytes)]
           (/ (first seq) (second seq)))
 [r] (msg/pack [(numerator r) (denominator r)]))

(extend-msgpack
 clojure.lang.IPersistentSet
 7
 [bytes] (set (msg/unpack bytes))
 [s] (msg/pack (seq s)))
