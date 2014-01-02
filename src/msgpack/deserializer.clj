(ns msgpack.deserializer
  (:require [msgpack.utils :refer :all]
            [msgpack.ext :refer :all]))

(defn- as-unsigned [byte] (bit-and 0xff byte))

(defn deserialize [bytes]
  (let [first-byte (-> bytes first as-unsigned)]
    (cond
      (<= 0x00 first-byte 0x7f) nil
      (<= 0x80 first-byte 0x8f) nil
      (<= 0x90 first-byte 0x9f) nil
      (<= 0xa0 first-byte 0xbf) nil
      (= first-byte 0xc0) nil
      (= first-byte 0xc1) nil
      (= first-byte 0xc2) nil
      (= first-byte 0xc3) nil
      (= first-byte 0xc4) nil
      (= first-byte 0xc5) nil
      (= first-byte 0xc6) nil
      (= first-byte 0xc7) nil
      (= first-byte 0xc8) nil
      (= first-byte 0xc9) nil
      (= first-byte 0xca) nil
      (= first-byte 0xcb) nil
      (= first-byte 0xcc) nil
      (= first-byte 0xcd) nil
      (= first-byte 0xce) nil
      (= first-byte 0xcf) nil
      (= first-byte 0xd0) nil
      (= first-byte 0xd1) nil
      (= first-byte 0xd2) nil
      (= first-byte 0xd3) nil
      (= first-byte 0xd4) nil
      (= first-byte 0xd5) nil
      (= first-byte 0xd6) nil
      (= first-byte 0xd7) nil
      (= first-byte 0xd8) nil
      (= first-byte 0xd9) nil
      (= first-byte 0xda) nil
      (= first-byte 0xdb) nil
      (= first-byte 0xdc) nil
      (= first-byte 0xdd) nil
      (= first-byte 0xde) nil
      (= first-byte 0xdf) nil
      (<= 0xe0 first-byte 0xff) nil)))
