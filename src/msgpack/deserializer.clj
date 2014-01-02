(ns msgpack.deserializer
  (:require [msgpack.utils :refer :all]
            [msgpack.ext :refer :all]))

(defn- as-unsigned [byte] (bit-and 0xff byte))

(defn- deserialize-positive-fixint [bytes]
  (bit-and 0x7f (first bytes)))

(defn- ??? [] (throw (UnsupportedOperationException.)))

(defn deserialize [bytes]
  (let [first-byte (-> bytes first as-unsigned)]
    (cond
      (<= 0x00 first-byte 0x7f) (deserialize-positive-fixint bytes)
      (<= 0x80 first-byte 0x8f) (???)
      (<= 0x90 first-byte 0x9f) (???)
      (<= 0xa0 first-byte 0xbf) (???)
      (= first-byte 0xc0) nil
      (= first-byte 0xc2) false
      (= first-byte 0xc3) true
      (= first-byte 0xc4) (???)
      (= first-byte 0xc5) (???)
      (= first-byte 0xc6) (???)
      (= first-byte 0xc7) (???)
      (= first-byte 0xc8) (???)
      (= first-byte 0xc9) (???)
      (= first-byte 0xca) (???)
      (= first-byte 0xcb) (???)
      (= first-byte 0xcc) (???)
      (= first-byte 0xcd) (???)
      (= first-byte 0xce) (???)
      (= first-byte 0xcf) (???)
      (= first-byte 0xd0) (???)
      (= first-byte 0xd1) (???)
      (= first-byte 0xd2) (???)
      (= first-byte 0xd3) (???)
      (= first-byte 0xd4) (???)
      (= first-byte 0xd5) (???)
      (= first-byte 0xd6) (???)
      (= first-byte 0xd7) (???)
      (= first-byte 0xd8) (???)
      (= first-byte 0xd9) (???)
      (= first-byte 0xda) (???)
      (= first-byte 0xdb) (???)
      (= first-byte 0xdc) (???)
      (= first-byte 0xdd) (???)
      (= first-byte 0xde) (???)
      (= first-byte 0xdf) (???)
      (<= 0xe0 first-byte 0xff) (???))))
