(ns msgpack.clojure-extensions
  "Extended types for Clojure-specific types"
  (:require [msgpack.core :as msg]
            [msgpack.macros :refer [extend-msgpack]]))

(extend-msgpack
 clojure.lang.Keyword
 3
 [bytes] (keyword (msg/unpack bytes))
 [kw] (msg/pack (name kw)))

(extend-msgpack
 clojure.lang.Symbol
 4
 [bytes] (symbol (msg/unpack bytes))
 [sym] (msg/pack (name sym)))

(extend-msgpack
 java.lang.Character
 6
 [bytes] (first (char-array (msg/unpack bytes)))
 [c] (msg/pack (str c)))
