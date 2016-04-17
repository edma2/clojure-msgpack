# clojure-msgpack

clojure-msgpack is a lightweight and simple library for converting
between native Clojure data structures and MessagePack byte formats.
clojure-msgpack only depends on Clojure itself; it has no third-party
dependencies.

## Installation

Get it from clojars: https://clojars.org/clojure-msgpack
![Clojars Project](http://clojars.org/clojure-msgpack/latest-version.svg)

## Usage

### Basic
* ```pack```: Serialize object as a sequence of java.lang.Bytes.
* ```unpack``` Deserialize bytes as a Clojure object.
```clojure
(require '[msgpack.core :as msg])
(require 'msgpack.clojure-extensions)

(msg/pack {:compact true :schema 0})
; => #<byte[] [B@60280b2e>

(msg/unpack (msg/pack {:compact true :schema 0}))
; => {:schema 0, :compact true}
`````

### Streaming
* ```unpack-stream```: Takes a [java.io.DataInput](http://docs.oracle.com/javase/7/docs/api/java/io/DataInput.html) as an argument. Usually you wrap this around an [InputStream](http://docs.oracle.com/javase/7/docs/api/java/io/InputStream.html)
* ```pack-stream```: Takes a [java.io.DataOutput](http://docs.oracle.com/javase/7/docs/api/java/io/DataOutput.html) as an argument. Usually you wrap this around an [OutputStream](http://docs.oracle.com/javase/7/docs/api/java/io/OutputStream.html)
```clojure
(use 'clojure.java.io)
(import '(java.io.DataOutputStream) '(java.io.DataInputStream))

(with-open [o (output-stream "test.dat")]
  (let [data-output (java.io.DataOutputStream. o)]
    (msg/pack-stream {:compact true :schema 0} data-output)))

(with-open [i (input-stream "test.dat")]
  (let [data-input (java.io.DataInputStream. i)]
    (msg/unpack-stream data-input)))
; => {:schema 0, :compact true}
```

### Core types

Clojure			                | MessagePack
----------------------------|------------
nil			                    | Nil
java.lang.Boolean	          | Boolean
java.lang.Byte	            | Integer
java.lang.Short	            | Integer
java.lang.Integer	          | Integer
java.lang.Long	            | Integer
java.lang.BigInteger	      | Integer
clojure.lang.BigInt	        | Integer
java.lang.Float		          | Float
java.lang.Double	          | Float
java.math.BigDecimal	      | Float
java.lang.String	          | String
clojure.lang.Sequential	    | Array
clojure.lang.IPersistentMap | Map
msgpack.core.Ext	          | Extended

Serializing a value of unrecognized type will fail with `IllegalArgumentException`.  See [Application types](#application-types) if you want to register your own types.

### Clojure types
Some native Clojure types don't have an obvious MessagePack counterpart. We can
serialize them as Extended types. To enable automatic conversion of these
types, load the `clojure-extensions` library.

Clojure			    | MessagePack
----------------------------|------------
clojure.lang.Keyword	    | Extended (type = 3)
clojure.lang.Symbol	    | Extended (type = 4)
java.lang.Character	    | Extended (type = 5)
clojure.lang.Ratio	    | Extended (type = 6)
clojure.lang.IPersistentSet | Extended (type = 7)

With `msgpack.clojure-extensions`:
```clojure
(require 'msgpack.clojure-extensions)
(msg/pack :hello)
; => #<byte[] [B@a8c55bf>
```

Without `msgpack.clojure-extensions`:
```clojure
(msg/pack :hello)
; => IllegalArgumentException No implementation of method: :pack-stream of
; protocol: #'msgpack.core/Packable found for class: clojure.lang.Keyword
; clojure.core/-cache-protocol-fn (core _deftype.clj:544)
```

### <a name="application-types">Application types</a>
You can also define your own Extended types with `extend-msgpack`.

```clojure
(require '[msgpack.macros :refer [extend-msgpack]])

(defrecord Person [name])

(extend-msgpack
  Person
  100
  [p] (.getBytes (:name p))
  [bytes] (->Person (String. bytes)))

(msg/unpack (msg/pack [(->Person "bob") 5 "test"]))
; => (#user.Person{:name "bob"} 5 "test")
```

### Options
All pack and unpack functions take an optional map of options:
* `:compatibility-mode`
  Serialize/deserialize strings and bytes using the raw-type defined here:
  https://github.com/msgpack/msgpack/blob/master/spec-old.md

  Note: No error is thrown if an unpacked value is reserved under the old spec
  but defined under the new spec. We always deserialize something if we can
  regardless of `compatibility-mode`.

```clojure
(msg/pack (byte-array (byte 9)) {:compatibility-mode true})
```

## License
clojure-msgpack is MIT licensed. See the included LICENSE file for more details.
