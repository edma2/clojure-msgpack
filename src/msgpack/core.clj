(ns msgpack.core
  (:require [msgpack.util :refer :all])
  (:import java.io.ByteArrayOutputStream
           java.io.DataOutputStream))

(declare pack-int pack-bytes pack-all)

(defprotocol Packable
  "An object that can be encoded in a MessagePack format."
  (pack [this]))

(defrecord Extension [type data])

(defmacro defext
  "Treat an existing class as a MessagePack extended type.
  As a side-effect, the class will extend the Packable protocol.

  (defstruct Employee [name])

  (defext Employee 1
    [e] (.getBytes (:name e)))

  expands into:

  (extend-protocol Packable
    Employee
    (pack [e] (pack (Extension. 1 (.getBytes (:name e))))))

  and this will work:
  (pack (Employee. employee-name))"
  [class type args body]
  (assert (<= 0 type 127)
          "[-1, -128]: reserved for future pre-defined extensions.")
  `(extend-protocol Packable
     ~class
     (pack ~args (pack (Extension. ~type ~body)))))

(extend-protocol Packable
  nil
  (pack [_] (byte-array [(B 0xc0)]))

  Boolean
  (pack [b]
    (if b (byte-array [(B 0xc3)]) (byte-array [(B 0xc2)])))

  Byte (pack [n] (pack-int n))
  Short (pack [n] (pack-int n))
  Integer (pack [n] (pack-int n))
  Long (pack [n] (pack-int n))

  clojure.lang.BigInt
  (pack [n]
    (if (<= 0x8000000000000000 n 0xffffffffffffffff)
      (byte-array (cons (B 0xcf) (long->bytes (.longValue n))))
      (pack (.longValue n))))

  Float
  (pack [x]
    (byte-array (cons (B 0xca) (float->bytes x))))

  Double
  (pack [x]
    (byte-array (cons (B 0xcb) (double->bytes x))))

  clojure.lang.Keyword (pack [k] (pack (name k)))
  clojure.lang.Symbol (pack [s] (pack (name s)))
  String
  (pack [s]
    (let [bytes (.getBytes s)
          len (count bytes)]
      (cond
        (<= len 0x1f)       (byte-array (cons (B (bit-or 2r10100000 len)) bytes))
        (<= len 0xff)       (byte-array (concat [(B 0xd9)] (byte->bytes len) bytes))
        (<= len 0xffff)     (byte-array (concat [(B 0xda)] (short->bytes len) bytes))
        (<= len 0xffffffff) (byte-array (concat [(B 0xdb)] (int->bytes len) bytes)))))

  clojure.lang.Sequential
  (pack [seq]
    (let [len (count seq)
          bytes (pack-all seq)]
      (cond
        (<= len 0xf)        (byte-array (cons (B (bit-or 2r10010000 len)) bytes))
        (<= len 0xffff)     (byte-array (concat [(B 0xdc)] (short->bytes len) bytes))
        (<= len 0xffffffff) (byte-array (concat [(B 0xdd)] (int->bytes len) bytes)))))

  clojure.lang.IPersistentMap
  (pack [map]
    (let [len (count map)
          bytes (pack-all (interleave (keys map) (vals map)))]
      (cond
        (<= len 0xf)        (byte-array (cons (B (bit-or 2r10000000 len)) bytes))
        (<= len 0xffff)     (byte-array (concat [(B 0xde)] (short->bytes len) bytes))
        (<= len 0xffffffff) (byte-array (concat [(B 0xdf)] (int->bytes len) bytes)))))

  Extension
  (pack [ext]
    (let [type (:type ext)
          bytes (:data ext)
          len (count bytes)]
      (cond
        (= len 1)           (byte-array (concat [(B 0xd4) (B type)] bytes))
        (= len 2)           (byte-array (concat [(B 0xd5) (B type)] bytes))
        (= len 4)           (byte-array (concat [(B 0xd6) (B type)] bytes))
        (= len 8)           (byte-array (concat [(B 0xd7) (B type)] bytes))
        (= len 16)          (byte-array (concat [(B 0xd8) (B type)] bytes))
        (<= len 0xff)       (byte-array (concat (cons (B 0xc7) (byte->bytes len)) (cons (B type) bytes)))
        (<= len 0xffff)     (byte-array (concat (cons (B 0xc8) (short->bytes len)) (cons (B type) bytes)))
        (<= len 0xffffffff) (byte-array (concat (cons (B 0xc9) (int->bytes len)) (cons (B type) bytes)))))))

;; Clojure bug when extending primitive types inside extend-protocol.
;; https://groups.google.com/forum/#!msg/clojure/PwmzA12By-I/nYBdNu2IeyMJ
(extend-type (class (java.lang.reflect.Array/newInstance Byte 0))
  Packable
  (pack [bytes] (pack-bytes bytes)))

(extend-type (Class/forName "[B")
  Packable
  (pack [bytes] (pack-bytes bytes)))

(defn- pack-all
  "Recursively serialize a collection of items, then concatenate the
  result."
  [coll]
  (apply concat (map pack coll)))

(defn- pack-int [n]
  (cond
    (<= 0 n 127)                  (byte-array (byte->bytes n))
    (<= -32 n -1)                 (byte-array (byte->bytes n))
    (<= 0 n 0xff)                 (byte-array (cons (B 0xcc) (byte->bytes n)))
    (<= 0 n 0xffff)               (byte-array (cons (B 0xcd) (short->bytes n)))
    (<= 0 n 0xffffffff)           (byte-array (cons (B 0xce) (int->bytes n)))
    (<= 0 n 0x7fffffffffffffff)   (byte-array (cons (B 0xcf) (long->bytes n)))
    (<= -0x80 n -1)               (byte-array (cons (B 0xd0) (byte->bytes n)))
    (<= -0x8000 n -1)             (byte-array (cons (B 0xd1) (short->bytes n)))
    (<= -0x80000000 n -1)         (byte-array (cons (B 0xd2) (int->bytes n)))
    (<= -0x8000000000000000 n -1) (byte-array (cons (B 0xd3) (long->bytes n)))))

(defn- pack-bytes [bytes]
  (let [len (count bytes)]
    (cond
      (<= len 0xff)       (byte-array (concat [(B 0xc4)] (byte->bytes len) bytes))
      (<= len 0xffff)     (byte-array (concat [(B 0xc5)] (short->bytes len) bytes))
      (<= len 0xffffffff) (byte-array (concat [(B 0xc6)] (int->bytes len) bytes)))))
