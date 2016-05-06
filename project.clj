(defproject clojure-msgpack "1.2.0"
  :description "A lightweight Clojure implementation of the MessagePack spec."
  :url "https://github.com/edma2/clojure-msgpack"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]]
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
   :test {:dependencies [[org.clojure/test.check "0.9.0"]]}}
  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-3"]]
  :figwheel
  {:nrepl-port 7888}
  :cljsbuild
  {:builds
   [{:id "test"
     :source-paths ["src" "test"]
     :figwheel true
     :compiler
     {:main "msgpack.core-test"
      :asset-path "js/out"
      :output-to "resources/public/js/msgpack.js"
      :output-dir "resources/public/js/out" }}]})
