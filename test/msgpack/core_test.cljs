(ns msgpack.core-test
  (:refer-clojure :exclude [byte])
  (:require-macros
   [msgpack.core-test :refer [round-trip-cljs]])
  (:require [msgpack.core :as msg]
            [msgpack.cljs-extensions]
            [clojure.walk :refer [postwalk]]
            [cljs.test :refer-macros [deftest is testing run-tests]]
            [cljs.test :as t]))

(enable-console-print!)

(extend-protocol ISeqable
  js/ArrayBuffer
  (-seq [this]
    (array-seq (js/Uint8ClampedArray. this)))
  js/Uint8ClampedArray
  (-seq [this]
    (array-seq this)))

(defn- byte [x]
  (if (number? x) x (.charCodeAt (str x) 0)))

(defn- fill-string [n c]
  (apply str (repeat n c)))

(defn byte-array [x]
  (if (number? x)
    (js/Uint8ClampedArray. x)
    (js/Uint8ClampedArray. (clj->js x))))

(def unsigned-byte-array byte-array)

(defn normalize-cljs
  "Equality is not defined for typed arrays. Instead convert them into sequences
  and compare them that way."
  [v]
  (postwalk
   (fn [v]
     (cond
       (instance? js/Uint8ClampedArray v) (seq v)
       :else v))
   v))

(deftest nil-test
  (testing "nil"
    (round-trip-cljs nil [0xc0])))

(deftest boolean-test
  (testing "booleans"
    (round-trip-cljs false [0xc2])
    (round-trip-cljs true [0xc3])))

(deftest int-test
  (testing "positive fixnum"
    (round-trip-cljs 0 [0x00])
    (round-trip-cljs 0x10 [0x10])
    (round-trip-cljs 0x7f [0x7f]))
  (testing "negative fixnum"
    (round-trip-cljs -1 [0xff])
    (round-trip-cljs -16 [0xf0])
    (round-trip-cljs -32 [0xe0]))
  (testing "uint 8"
    (round-trip-cljs 0x80 [0xcc 0x80])
    (round-trip-cljs 0xf0 [0xcc 0xf0])
    (round-trip-cljs 0xff [0xcc 0xff]))
  (testing "uint 16"
    (round-trip-cljs 0x100 [0xcd 0x01 0x00])
    (round-trip-cljs 0x2000 [0xcd 0x20 0x00])
    (round-trip-cljs 0xffff [0xcd 0xff 0xff]))
  (testing "uint 32"
    (round-trip-cljs 0x10000 [0xce 0x00 0x01 0x00 0x00])
    (round-trip-cljs 0x200000 [0xce 0x00 0x20 0x00 0x00])
    (round-trip-cljs 0xffffffff [0xce 0xff 0xff 0xff 0xff]))
  (testing "int 8"
    (round-trip-cljs -33 [0xd0 0xdf])
    (round-trip-cljs -100 [0xd0 0x9c])
    (round-trip-cljs -128 [0xd0 0x80]))
  (testing "int 16"
    (round-trip-cljs -129 [0xd1 0xff 0x7f])
    (round-trip-cljs -2000 [0xd1 0xf8 0x30])
    (round-trip-cljs -32768 [0xd1 0x80 0x00]))
  (testing "int 32"
    (round-trip-cljs -32769 [0xd2 0xff 0xff 0x7f 0xff])
    (round-trip-cljs -1000000000 [0xd2 0xc4 0x65 0x36 0x00])
    (round-trip-cljs -2147483648 [0xd2 0x80 0x00 0x00 0x00])))

(deftest float-test
  (testing "float 64"
    (round-trip-cljs (double 2.5) [0xcb 0x40 0x04 0x00 0x00 0x00 0x00 0x00 0x00])
    (round-trip-cljs 1e-46 [0xcb 0x36 0x62 0x44 0xce 0x24 0x2c 0x55 0x61])))

(deftest str-test
  (testing "fixstr"
    (round-trip-cljs "hello world" [0xab 0x68 0x65 0x6c 0x6c 0x6f 0x20 0x77 0x6f 0x72 0x6c 0x64])
    (round-trip-cljs "" [0xa0])
    (round-trip-cljs "abc" [0xa3 0x61 0x62 0x63])
    (round-trip-cljs "äöü" [0xa6 0xc3 0xa4 0xc3 0xb6 0xc3 0xbc])
    (round-trip-cljs (fill-string 31 \a)
                     (cons 0xbf (repeat 31 0x61))))
  (testing "str 8"
    (round-trip-cljs (fill-string 32 \b)
                     (concat [0xd9 0x20] (repeat 32 (byte \b))))
    (round-trip-cljs (fill-string 100 \c)
                     (concat [0xd9 0x64] (repeat 100 (byte \c))))
    (round-trip-cljs (fill-string 255 \d)
                     (concat [0xd9 0xff] (repeat 255 (byte \d)))))
  (testing "str 16"
    (round-trip-cljs (fill-string 256 \b)
                     (concat [0xda 0x01 0x00] (repeat 256 (byte \b))))
    (round-trip-cljs (fill-string 65535 \c)
                     (concat [0xda 0xff 0xff] (repeat 65535 (byte \c)))))
  (testing "str 32"
    (round-trip-cljs (fill-string 65536 \b)
                     (concat [0xdb 0x00 0x01 0x00 0x00] (repeat 65536 (byte \b))))))

(deftest bin-test
  (testing "bin 8"
    (round-trip-cljs (byte-array 0) [0xc4 0x00])
    (round-trip-cljs (unsigned-byte-array [0x80]) [0xc4 0x01 0x80])
    (round-trip-cljs (unsigned-byte-array (repeat 32 0x80))
                     (concat [0xc4 0x20] (repeat 32 0x80)))
    (round-trip-cljs (unsigned-byte-array (repeat 255 0x80))
                     (concat [0xc4 0xff] (repeat 255 0x80))))
  (testing "bin 16"
    (round-trip-cljs (unsigned-byte-array (repeat 256 0x80))
                     (concat [0xc5 0x01 0x00] (repeat 256 0x80))))
  (testing "bin 32"
    (round-trip-cljs (unsigned-byte-array (repeat 65536 0x80))
                     (concat [0xc6 0x00 0x01 0x00 0x00] (repeat 65536 0x80)))))

(deftest clojurescript-test
  (testing "cljs.core.Keyword"
    (round-trip-cljs :abc [0xd6 0x3 0xa3 0x61 0x62 0x63]))
  (testing "cljs.core.Symbol"
    (round-trip-cljs 'abc [0xd6 0x4 0xa3 0x61 0x62 0x63]))
  (testing "cljs.core.PersistentHashSet"
    (round-trip-cljs #{} [0xd4 0x7 0xc0])))

(deftest array-test
  (testing "fixarray"
    (round-trip-cljs '() [0x90])
    (round-trip-cljs  [] [0x90])
    (round-trip-cljs [[]] [0x91 0x90])
    (round-trip-cljs [5 "abc" true] [0x93 0x05 0xa3 0x61 0x62 0x63 0xc3])
    #_(round-trip-cljs [true 1 (msg/->Ext 100 (.getBytes "foo")) 0xff {1 false 2 "abc"} (unsigned-byte-array [0x80]) [1 2 3] "abc"]
                       [0x98 0xc3 0x1 0xc7 0x3 0x64 0x66 0x6f 0x6f 0xcc 0xff 0x82 0x1 0xc2 0x2 0xa3 0x61 0x62 0x63 0xc4 0x1 0x80 0x93 0x1 0x2 0x3 0xa3 0x61 0x62 0x63]))
  (testing "array 16"
    (round-trip-cljs (repeat 16 5)
                     (concat [0xdc 0x00 0x10] (repeat 16 5)))
    (round-trip-cljs (repeat 65535 5)
                     (concat [0xdc 0xff 0xff] (repeat 65535 5))))
  (testing "array 32"
    (round-trip-cljs (repeat 65536 5)
                     (concat [0xdd 0x00 0x01 0x00 0x00] (repeat 65536 5)))))

(deftest map-test
  (testing "fixmap"
    (round-trip-cljs {} [0x80])
    (round-trip-cljs {1 true 2 "abc" 3 (unsigned-byte-array [0x80])}
                     [0x83 0x01 0xc3 0x02 0xa3 0x61 0x62 0x63 0x03 0xc4 0x01 0x80])
    (round-trip-cljs {"abc" 5} [0x81 0xa3 0x61 0x62 0x63 0x05])
    (round-trip-cljs {(unsigned-byte-array [0x80]) 0xffff}
                     [0x81 0xc4 0x01 0x80 0xcd 0xff 0xff])
    (round-trip-cljs {true nil} [0x81 0xc3 0xc0])
    (round-trip-cljs {"compact" true "schema" 0}
                     [0x82 0xa7 0x63 0x6f 0x6d 0x70 0x61 0x63 0x74 0xc3 0xa6 0x73 0x63 0x68 0x65 0x6d 0x61 0x00])
    #_(round-trip-cljs {1 [{1 2 3 4} {}], 2 1, 3 [false "def"], 4 {0x100000000 "a" 0xffffffff "b"}}
                     [0x84 0x01 0x92 0x82 0x01 0x02 0x03 0x04 0x80 0x02 0x01 0x03 0x92 0xc2 0xa3 0x64 0x65 0x66 0x04 0x82 0xcf 0x00 0x00 0x00 0x01 0x00 0x00 0x00 0x00 0xa1 0x61 0xce 0xff 0xff 0xff 0xff 0xa1 0x62]))
  (testing "map 16"
    (round-trip-cljs (apply sorted-map (interleave (range 0 16) (repeat 16 5)))
                     [0xde 0 16 0 5 1 5 2 5 3 5 4 5 5 5 6 5 7 5 8 5 9 5 10 5 11 5 12 5 13 5 14 5 15 5])))
