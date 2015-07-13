(set-env!
 :source-paths   #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [prismatic/plumbing "0.4.4"]
                 [ossicone "0.1.0-SNAPSHOT"]
                 [jamesmacaulay/zelkova "0.4.0"]

                 [org.clojure/tools.nrepl "0.2.10" :scope "test"]
                 [adzerk/boot-cljs "0.0-3308-0" :scope "test"]
                 [adzerk/boot-cljs-repl "0.1.10-SNAPSHOT" :scope "test"]
                 [adzerk/boot-reload "0.3.1" :scope "test"]
                 [pandeiro/boot-http "0.6.3-SNAPSHOT" :scope "test"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 '[pandeiro.boot-http    :refer [serve]])

(deftask dev []
  (set-env! :source-paths #{"src" "dev"})
  (comp (serve "target")
     (watch)
     (reload :on-jsload 'cljs.user/main)
     (cljs-repl)
     (cljs :optimizations :none
           :source-map true
           :pretty-print true)))

(deftask build []
  (set-env! :resource-paths #{"src"})
  (comp (pom :project 'desmo
          :version "0.1.0-SNAPSHOT")
     (jar)
     (install)))
