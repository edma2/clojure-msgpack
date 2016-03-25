(defproject clojure-msgpack "1.1.2"
  :description "A lightweight Clojure implementation of the MessagePack spec."
  :url "https://github.com/edma2/clojure-msgpack"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :global-vars {*warn-on-reflection* true}
  :scm {:name "git"
        :url "https://github.com/edma2/clojure-msgpack"}
  :profiles
  {:non-utf8-encoding
   {:jvm-opts
    ["-Dfile.encoding=ISO-8859-1"]}
   :eastwood {:plugins [[jonase/eastwood "0.2.3"]]
              :dependencies [[org.clojure/test.check "0.9.0"]]
              :eastwood {:config-files ["eastwood.clj"]}}
   :test {:dependencies [[org.clojure/test.check "0.9.0"]]}})
