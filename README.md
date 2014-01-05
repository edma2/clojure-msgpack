# clojure-msgpack

clojure-msgpack is a library for
* serializing native Clojure data structures as MessagePack byte arrays.
* deserializing MessagePack byte arrays into native Clojure data structures.

## Usage

```clojure
(require '[msgpack.core :refer :all])

(pack {:compact true :schema 0})
; #<byte[] [B@5984b649>

; Declare records and types as Extended types.
(defext Employee 1 [e]
  (.getBytes (:name e)))

(let [bob (Employee. "Bob")
      bytes (pack bob)]
  (map #(format "0x%x" %) bytes))
; ("0xc7" "0x3" "0x1" "0x62" "0x6f" "0x62")

; Or encode an extension directly
(pack (Extension. 2 (.getBytes "test")))
```

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
