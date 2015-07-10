(ns desmo.core
  (:require
   [nuejure.core :refer :all]))

(defmacro component [bindings & body]
  (let [last (list `(return ~(last body)))
        body (concat (butlast body) last)]
    `(mlet ~bindings ~@body)))

(defmacro defc [name bindings & body]
  `(def ~name (component ~bindings ~@body)))
