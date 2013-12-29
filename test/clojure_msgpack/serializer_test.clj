(ns clojure-msgpack.serializer-test
  (:require [clojure.test :refer :all]
            [clojure-msgpack.utils :refer :all]
            [clojure-msgpack.serializer :refer :all]))

(deftest nil-test
  (testing "nil"
    (is (byte-literals [0xc0]) (serialize nil))))
