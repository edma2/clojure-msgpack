# clojure-msgpack

clojure-msgpack is a lightweight and simple library for converting
between native Clojure data structures and MessagePack byte formats.
clojure-msgpack only depends on Clojure itself; it has zero third-party
dependencies.

## Installation

Get it from clojars: https://clojars.org/clojure-msgpack
![Clojars Project](http://clojars.org/clojure-msgpack/latest-version.svg)

## Usage

### Basic:
* ```pack```: Serialize object as a sequence of java.lang.Bytes.
* ```unpack``` Deserialize bytes as a Clojure object.
```clojure
user=> (require '[msgpack.core :refer :all])
nil

user=> (pack {:compact true :schema 0})
(-126 -90 115 99 104 101 109 97 0 -89 99 111 109 112 97 99 116 -61)

user=> (unpack (pack {:compact true :schema 0}))
{"schema" 0, "compact" true}
`````

### Streaming:
* ```unpack-stream```: Takes a [java.io.DataInput](http://docs.oracle.com/javase/7/docs/api/java/io/DataInput.html) as an argument. Usually you want to wrap this around some sort of [InputStream](http://docs.oracle.com/javase/7/docs/api/java/io/InputStream.html)
* ```pack-stream```: Takes a [java.io.DataOutput](http://docs.oracle.com/javase/7/docs/api/java/io/DataOutput.html) as an argument. Usually you want to wrap this around some sort of [OutputStream](http://docs.oracle.com/javase/7/docs/api/java/io/OutputStream.html)
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
A macro ```defext``` is provided to allow serialization of application-specific types. Currently this only works one-way; data serialized this way will always deserialize as a raw ```Extended``` type.
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

### Extras:
Symbols and Keywords are treated as MessagePack strings.
Sets are treated as MessagePack arrays.

## TODO
* Error checking
* Compatibility mode
* Benchmarks

## License

clojure-msgpack is MIT licensed. See the included LICENSE file for more details.
