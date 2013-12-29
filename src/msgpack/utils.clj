;; Utilities for specifying bytes as if they were hex literals.
;; Ex. (byte 0xc0) will produce a cast error. (byte-literal 0xc0) will perform
;; the required math needed to fit the bit-pattern 0xc0 into a Java byte.
(ns msgpack.utils)

(defn byte-literal [x]
  (if (> 127 x)
    (byte (- x 256))
    (byte x)))

(defn byte-literals [xs]
  (map byte-literal xs))
