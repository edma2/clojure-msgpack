# clojure-msgpack

clojure-msgpack is a library for
* serializing native Clojure data structures as MessagePack byte arrays.
* deserializing MessagePack byte arrays into native Clojure data structures.

## Installation

See https://clojars.org/clojure-msgpack

## Usage

### Basic:
```clojure
user=> (require '[msgpack.core :refer :all])
nil

user=> (pack {:compact true :schema 0})
(-126 -90 115 99 104 101 109 97 0 -89 99 111 109 112 97 99 116 -61)

user=> (unpack (pack {:compact true :schema 0}))
{"schema" 0, "compact" true}
`````

### Streaming:
```clojure
user=> (use 'clojure.java.io)
nil

user=> (import '(java.io.DataOutputStream) '(java.io.DataInputStream))
nil

user=> (with-open [o (output-stream "test.dat")]
  (let [data-output (DataOutputStream. o)]
    (pack-stream {:compact true :schema 0} data-output)))
nil

user=> (with-open [i (input-stream "test.dat")]
  (let [data-input (DataInputStream. i)]
    (unpack-stream data-input)))
{"schema" 0, "compact" true}
```

### User-defined extensions:
```clojure
user=> (require '[msgpack.macros :refer [defext]])
nil

user=> (defrecord Employee [name])
user.Employee

user=> (defext Employee 1 #(.getBytes (:name %)))
nil

user=> (let [bob (Employee. "Bob")
             bytes (pack bob)]
         (map #(format "0x%x" %) bytes))
("0xc7" "0x3" "0x1" "0x42" "0x6f" "0x62")
```

## TODO:
* Error checking
* Compatibility mode
* Benchmarks

## License

clojure-msgpack is MIT licensed. See the included LICENSE file for more details.
