# clojure-msgpack

clojure-msgpack is a library for
* serializing native Clojure data structures as MessagePack byte arrays.
* deserializing MessagePack byte arrays into native Clojure data structures.

## Installation

See https://clojars.org/clojure-msgpack

## Usage

```clojure
(require '[msgpack.core :refer :all])

(pack {:compact true :schema 0})
; #<byte[] [B@5984b649>

; Declare records and types as Extended types.
(defrecord Employee [name])

(defext Employee 1 [e]
  (.getBytes (:name e)))

(let [bob (Employee. "Bob")
      bytes (pack bob)]
  (map #(format "0x%x" %) bytes))
; ("0xc7" "0x3" "0x1" "0x62" "0x6f" "0x62")

; Or encode an extension directly
(pack (->Extension 1 (.getBytes "Bob")))

(pack {:compact true :schema 0})
; #<byte[] [B@59ce1eed>

(unpack (pack {:compact true :schema 0}) :keywordize)
; {:schema 0, :compact true}
```

## License

clojure-msgpack is MIT licensed. See the included LICENSE file for more details.
