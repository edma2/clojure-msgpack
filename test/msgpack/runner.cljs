(ns msgpack.runner
  (:require  [msgpack.core-test]
             [doo.runner :refer-macros [doo-tests]]))

(doo-tests 'msgpack.core-test)
