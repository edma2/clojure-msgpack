# clojure-msgpack

clojure-msgpack is a library for
* serializing native Clojure data structures as MessagePack byte arrays.
* deserializing MessagePack byte arrays into native Clojure data structures.

## Usage

```clojure
(require '[msgpack.core :as msgpack])
(msgpack/serialize {:compact true :schema 0}) ; Returns an array of bytes
(msgpack/deserialize raw-bytes) ; Accepts an array of bytes
```

clojure-msgpack supports extension types.
To use, simply extend the msgpack.proto/Extension protocol, which defines ```ext-data``` and ```ext-type```.
Read more about MessagePack extension types here: https://github.com/msgpack/msgpack/blob/master/spec.md#formats-ext)

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
