# clojure-msgpack

clojure-msgpack is a library for
* serializing native Clojure data structures as MessagePack byte sequences.
* deserializing MessagePack byte sequences into native Clojure data structures.

## Usage

```
(require '[msgpack.core :as msgpack])
; Returns a Seq of java.lang.Byte
(msgpack/serialize {:compact true :schema 0})
; Accepts a Seq of java.lang.Byte
(msgpack/deserialize raw-bytes)
```

If there are multiple ways of serializing a Clojure object the most compact
form is chosen.

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
