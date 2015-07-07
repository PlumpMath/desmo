(ns lollipop.core
  (:require
   [lollipop.dom :as dom]
   [plumbing.core :refer-macros [fn-> fn->>]]
   [weasel.repl :as repl]))

(defn term [p [k v]]
  {:update {:term-changed [p (partial vector k)]}
   :markup nil})

(defn terms [p ts]
  {:update {:term-removed [p (fn [k] (remove (comp (partial = k) first) ts))]}
   :markup (fn [] nil)})

(when-not (repl/alive?)
  (repl/connect "ws://localhost:9001"))
