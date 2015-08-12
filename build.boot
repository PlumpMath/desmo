(set-env!
 :source-paths   #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.48"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [prismatic/plumbing "0.4.4"]
                 [ossicone "0.1.0-SNAPSHOT"]
                 [jamesmacaulay/zelkova "0.4.0"]
                 [com.cognitect/transit-cljs "0.8.220"]
                 [cljsjs/virtual-dom "2.1.1-0"]

                 [adzerk/boot-cljs "1.7.48-SNAPSHOT" :scope "test"]
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
  (comp (serve :port 3002)
     (watch)
     (reload :on-jsload 'cljs.user/main)
     (cljs-repl)
     (cljs :optimizations :none
           :source-map true)))

(deftask install-jar []
  (set-env! :resource-paths #{"src"})
  (comp (pom :project 'desmo
          :version "0.2.1-SNAPSHOT")
     (jar)
     (install)))
