(ns msgpack.clojure-extensions
  "Extended types for Clojure-specific types"
  (:require [msgpack.core :as msg]
            [msgpack.macros :refer [extend-msgpack]]))

(defn- keyword->str
  "Convert keyword to string with namespace preserved.
  Example: :A/A => \"A/A\""
  [k]
  (subs (str k) 1))

(extend-msgpack
 clojure.lang.Keyword
 3
 [k] (msg/pack (keyword->str k))
 [bytes] (keyword (msg/unpack bytes)))

(extend-msgpack
 clojure.lang.Symbol
 4
 [s] (msg/pack (str s))
 [bytes] (symbol (msg/unpack bytes)))

(extend-msgpack
 java.lang.Character
 5
 [c] (msg/pack (str c))
 [bytes] (first (char-array (msg/unpack bytes))))

(extend-msgpack
 clojure.lang.Ratio
 6
 [r] (msg/pack [(numerator r) (denominator r)])
 [bytes] (let [seq (msg/unpack bytes)]
           (/ (first seq) (second seq))))

(extend-msgpack
 clojure.lang.IPersistentSet
 7
 [s] (msg/pack (seq s))
 [bytes] (set (msg/unpack bytes)))
