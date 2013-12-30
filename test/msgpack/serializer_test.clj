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
    ; 7-bit positive int
    (is (= (byte-literals [0x00]) (serialize 0)))
    (is (= (byte-literals [0x10]) (serialize 0x10)))
    (is (= (byte-literals [0x7f]) (serialize 0x7f)))
    ; 5-bit negative int
    (is (= (byte-literals [0xff]) (serialize -1)))
    (is (= (byte-literals [0xf0]) (serialize -16)))
    (is (= (byte-literals [0xe0]) (serialize -32)))
    ; 8-bit uint
    (is (= (byte-literals [0xcc 0x80]) (serialize 0x80)))
    (is (= (byte-literals [0xcc 0xf0]) (serialize 0xf0)))
    (is (= (byte-literals [0xcc 0xff]) (serialize 0xff)))
    ; 16-bit uint
    (is (= (byte-literals [0xcd 0x01 0x00]) (serialize 0x100)))
    (is (= (byte-literals [0xcd 0x20 0x00]) (serialize 0x2000)))
    (is (= (byte-literals [0xcd 0xff 0xff]) (serialize 0xffff)))
    ; 32-bit uint
    (is (= (byte-literals [0xce 0x00 0x01 0x00 0x00]) (serialize 0x10000)))
    (is (= (byte-literals [0xce 0x00 0x20 0x00 0x00]) (serialize 0x200000)))
    (is (= (byte-literals [0xce 0xff 0xff 0xff 0xff]) (serialize 0xffffffff)))
    ; 8-bit int
    (is (= (byte-literals [0xd0 0xdf]) (serialize -33)))
    (is (= (byte-literals [0xd0 0x9c]) (serialize -100)))
    (is (= (byte-literals [0xd0 0x80]) (serialize -128)))
    ; 16-bit int
    (is (= (byte-literals [0xd1 0xff 0x7f]) (serialize -129)))
    (is (= (byte-literals [0xd1 0xf8 0x30]) (serialize -2000)))
    (is (= (byte-literals [0xd1 0x80 0x00]) (serialize -32768)))
    ; 32-bit int
    (is (= (byte-literals [0xd2 0xff 0xff 0x7f 0xff]) (serialize -32769)))
    (is (= (byte-literals [0xd2 0xc4 0x65 0x36 0x00]) (serialize -1000000000)))
    (is (= (byte-literals [0xd2 0x80 0x00 0x00 0x00]) (serialize -2147483648)))
    ; 64-bit uint
    (is (= (byte-literals [0xcf 0x00 0x00 0x00 0x01 0x00 0x00 0x00 0x00])
           (serialize 0x100000000)))
    (is (= (byte-literals [0xcf 0x00 0x00 0x20 0x00 0x00 0x00 0x00 0x00])
           (serialize 0x200000000000)))
    (is (= (byte-literals [0xcf 0xff 0xff 0xff 0xff 0xff 0xff 0xff 0xff])
           (serialize 0xffffffffffffffff)))
    ; 64-bit int
    (is (= (byte-literals [0xd3 0xff 0xff 0xff 0xff 0x7f 0xff 0xff 0xff])
           (serialize -2147483649)))
    (is (= (byte-literals [0xd3 0xf2 0x1f 0x49 0x4c 0x58 0x9b 0xff 0xfe])
           (serialize -1000000000000000002)))
    (is (= (byte-literals [0xd3 0x80 0x00 0x00 0x00 0x00 0x00 0x00 0x00])
           (serialize -9223372036854775808)))))
