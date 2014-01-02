# clojure-msgpack

clojure-msgpack is a library for
* serializing native Clojure data structures as MessagePack byte arrays.
* deserializing MessagePack byte arrays into native Clojure data structures.

## Usage

```clojure
(require '[msgpack.core :as msgpack])
(require '[msgpack.ext :refer :all])

(msgpack/serialize {:compact true :schema 0})
; #<byte[] [B@5984b649>

(defrecord Person [name]
  Extended
  (extension [this] [1 (.getBytes (this name))]))

(let [bob (Person. "Bob")
      bytes (msgpack/serialize bob)]
  (map #(format "0x%x" %) bytes))
; ("0xc7" "0x3" "0x1" "0x62" "0x6f" "0x62")
```

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
