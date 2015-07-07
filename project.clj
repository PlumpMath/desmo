(defproject lollipop "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [prismatic/plumbing "0.4.4"]
                 [nuejure "0.1.0-SNAPSHOT"]
                 [cljs-ajax "0.3.13"]
                 [rum "0.2.7"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [jamesmacaulay/zelkova "0.4.0"]]
  :plugins [[lein-cljsbuild "1.0.6"]
            [lein-npm "0.5.0"]]
  :node-dependencies [[virtual-dom "2.0.1"]]
  :cljsbuild
  {:builds [{:source-paths ["src"]
             :compiler {:output-to  "resources/public/js/app.js"
                        :output-dir "resources/public/js/out"
                        :optimizations :none
                        :source-map true
                        :pretty-print  true
                        :warnings {:single-segment-namespace false}}}]}
  :profiles {:dev {:dependencies [[weasel "0.7.0"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [ring/ring-core "1.4.0-RC2"]
                                  [ring/ring-jetty-adapter "1.4.0-RC2"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :source-paths ["dev"]}})
