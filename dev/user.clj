(ns user
  (:require
   [weasel.repl.websocket :refer [repl-env]]
   [cemerick.piggieback :refer [cljs-repl]]
   [ring.util.response :refer [redirect]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.adapter.jetty :refer [run-jetty]]))

(defn start-cljs-repl []
  (-> (constantly (redirect "/index.html"))
      (wrap-resource "public")
      (wrap-content-type)
      (run-jetty {:port 3000 :join? false}))
  (cljs-repl (repl-env :ip "0.0.0.0" :port 9001)))
