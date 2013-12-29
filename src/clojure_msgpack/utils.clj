(ns clojure-msgpack.utils)

(defn byte-literal [x]
  (if (> 127 x)
    (byte (- x 256))
    (byte x)))

(defn byte-literals [xs]
  (map byte-literal xs))
