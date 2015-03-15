(ns msgpack.streaming-test
  (:require [clojure.test :refer :all]
            [msgpack.streaming :refer [pack]]))

(defn- byte-literals
  [bytes]
  (map unchecked-byte bytes))


(defmacro serializes-as [thing bytes]
  `(let [thing# ~thing
         bytes# (byte-literals ~bytes)]
     (is (= bytes# (pack thing#)))))

(deftest nil-test
  (testing "nil"
    (serializes-as nil [0xc0])))

(deftest boolean-test
  (testing "booleans"
    (serializes-as false [0xc2])
    (serializes-as true [0xc3])))
