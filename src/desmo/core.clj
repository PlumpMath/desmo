(ns desmo.core
  (:require
   [ossicone.core :refer :all]))

(defmacro on-let [bindings & body]
  `(let ~bindings (mdo ~@body)))

(defmacro component [bindings & body]
  (let [last (list `(return ~(last body)))
        body (concat (butlast body) last)]
    `(mlet ~bindings ~@body)))

(defmacro defc [name bindings & body]
  `(def ~name (component ~bindings ~@body)))
