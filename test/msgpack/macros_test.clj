(ns msgpack.macros-test
  (:require [clojure.test :refer :all]
            [msgpack.macros :refer [defext]]
            [msgpack.core :refer [pack unpack ->Extension]]
            [msgpack.core-test :refer [one-way byte-literals]]))

(defrecord Employee [name])

(defext Employee 5 #(.getBytes ^String (:name %)))

(deftest defext-test
  (testing "defext"
    (one-way (Employee. "bob") [0xc7 0x3 0x5 0x62 0x6f 0x62])
    (is (= (->Extension 5 (byte-literals [0x62 0x6f 0x62])) (unpack (pack (Employee. "bob")))))))
