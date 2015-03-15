(ns msgpack.streaming-test
  (:require [clojure.test :refer :all]
            [msgpack.streaming :refer [pack]]))

(defn- byte-literals
  [bytes]
  (map unchecked-byte bytes))

(deftest nil-test
  (testing "nil"
    (is (= (pack nil) (byte-literals [0xc0])))))

(deftest boolean-test
  (testing "booleans"
    (is (= (pack false) (byte-literals [0xc2])))
    (is (= (pack true) (byte-literals [0xc3])))))
