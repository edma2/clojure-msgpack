# clojure-msgpack

clojure-msgpack is a library for 
* encoding native Clojure data structures as MessagePack byte sequences.
* decoding MessagePack byte sequences into native Clojure data structures.

## Usage

=> (require ['clojure-msgpack.core :as 'msgpack])
=> (msgpack/encode {:compact true :schema 0}) ; returns a Seq of java.lang.Byte
=> (msgpack/decode raw-bytes) ; accepts any Seq of java.lang.Byte

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
