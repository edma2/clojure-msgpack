(ns msgpack.serializer-test
  (:require [clojure.test :refer :all]
            [msgpack.utils :refer :all]
            [msgpack.serializer :refer :all]))

(deftest nil-test
  (testing "nil"
    (is (= (byte-literals [0xc0]) (serialize nil)))))

(deftest boolean-test
  (testing "booleans"
    (is (= (byte-literals [0xc2]) (serialize false)))
    (is (= (byte-literals [0xc3]) (serialize true)))))

(deftest int-test
  (testing "ints"
    (is (= (byte-literals [0x00]) (serialize 0)))
    (is (= (byte-literals [0x10]) (serialize 0x10)))
    (is (= (byte-literals [0x7f]) (serialize 0x7f)))))
