(ns msgpack.core
  (:require [msgpack.io :refer :all]))

(declare pack-int pack-bytes pack-all)

(defprotocol Packable
  "An object that can be encoded in a MessagePack format."
  (pack [this]))

;; The raw extension type which consists of a (type, data) tuple
(defrecord Extension [type data])

(defmacro defext
  "Treat an existing class as a MessagePack extended type.
  As a side-effect, the class will extend the Packable protocol.

  (defstruct Employee [name])

  (defext Employee 1 [e]
    (.getBytes (:name e)))

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

(defmacro cond-let [bindings & clauses]
  `(let ~bindings (cond ~@clauses)))

(extend-protocol Packable
  nil
  (pack [_] (ubytes [0xc0]))

  Boolean
  (pack [b]
    (if b (ubytes [0xc3]) (ubytes [0xc2])))

  Byte (pack [n] (pack-int n))
  Short (pack [n] (pack-int n))
  Integer (pack [n] (pack-int n))
  Long (pack [n] (pack-int n))

  clojure.lang.BigInt
  (pack [n]
    (if (<= 0x8000000000000000 n 0xffffffffffffffff)
      (ubytes (cons 0xcf (long->bytes (.longValue n))))
      (pack (.longValue n))))

  Float
  (pack [x]
    (ubytes (cons 0xca (float->bytes x))))

  Double
  (pack [x]
    (ubytes (cons 0xcb (double->bytes x))))

  clojure.lang.Keyword (pack [k] (pack (name k)))
  clojure.lang.Symbol (pack [s] (pack (name s)))
  String
  (pack [s]
    (cond-let [bytes (.getBytes s)
               len (count bytes)]
      (<= len 0x1f)       (ubytes (cons (bit-or 2r10100000 len) bytes))
      (<= len 0xff)       (ubytes (concat [0xd9] (byte->bytes len) bytes))
      (<= len 0xffff)     (ubytes (concat [0xda] (short->bytes len) bytes))
      (<= len 0xffffffff) (ubytes (concat [0xdb] (int->bytes len) bytes))))

  clojure.lang.Sequential
  (pack [seq]
    (cond-let [len (count seq)
               bytes (pack-all seq)]
      (<= len 0xf)        (ubytes (cons (bit-or 2r10010000 len) bytes))
      (<= len 0xffff)     (ubytes (concat [0xdc] (short->bytes len) bytes))
      (<= len 0xffffffff) (ubytes (concat [0xdd] (int->bytes len) bytes))))

  clojure.lang.IPersistentMap
  (pack [map]
    (cond-let [len (count map)
               bytes (pack-all (interleave (keys map) (vals map)))]
      (<= len 0xf)        (ubytes (cons (bit-or 2r10000000 len) bytes))
      (<= len 0xffff)     (ubytes (concat [0xde] (short->bytes len) bytes))
      (<= len 0xffffffff) (ubytes (concat [0xdf] (int->bytes len) bytes))))

  Extension
  (pack [ext]
    (cond-let [type (:type ext)
               bytes (:data ext)
               len (count bytes)]
      (= len 1)           (ubytes (concat [0xd4 type] bytes))
      (= len 2)           (ubytes (concat [0xd5 type] bytes))
      (= len 4)           (ubytes (concat [0xd6 type] bytes))
      (= len 8)           (ubytes (concat [0xd7 type] bytes))
      (= len 16)          (ubytes (concat [0xd8 type] bytes))
      (<= len 0xff)       (ubytes (concat (cons 0xc7 (byte->bytes len)) (cons type bytes)))
      (<= len 0xffff)     (ubytes (concat (cons 0xc8 (short->bytes len)) (cons type bytes)))
      (<= len 0xffffffff) (ubytes (concat (cons 0xc9 (int->bytes len)) (cons type bytes))))))

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
    (<= 0 n 127)                  (ubytes (byte->bytes n))
    (<= -32 n -1)                 (ubytes (byte->bytes n))
    (<= 0 n 0xff)                 (ubytes (cons 0xcc (byte->bytes n)))
    (<= 0 n 0xffff)               (ubytes (cons 0xcd (short->bytes n)))
    (<= 0 n 0xffffffff)           (ubytes (cons 0xce (int->bytes n)))
    (<= 0 n 0x7fffffffffffffff)   (ubytes (cons 0xcf (long->bytes n)))
    (<= -0x80 n -1)               (ubytes (cons 0xd0 (byte->bytes n)))
    (<= -0x8000 n -1)             (ubytes (cons 0xd1 (short->bytes n)))
    (<= -0x80000000 n -1)         (ubytes (cons 0xd2 (int->bytes n)))
    (<= -0x8000000000000000 n -1) (ubytes (cons 0xd3 (long->bytes n)))))

(defn- pack-bytes [bytes]
  (cond-let [len (count bytes)]
    (<= len 0xff)       (ubytes (concat [0xc4] (byte->bytes len) bytes))
    (<= len 0xffff)     (ubytes (concat [0xc5] (short->bytes len) bytes))
    (<= len 0xffffffff) (ubytes (concat [0xc6] (int->bytes len) bytes))))
