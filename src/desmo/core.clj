(ns desmo.core
  (:require
   [ossicone.core :refer :all]))

(defn bindings [args]
  (let [s->b {'state (fn [[s]] [s `state])
              'conf (fn [[e]] [e `conf])
              'use (partial mapcat (fn [u] [u u]))
              'link (partial mapcat (fn [c] [c `(link ~(keyword c) ~c)]))}
        b? (set (keys s->b))]
    (->> args
         (filter (comp b? first))
         (mapcat (fn [[s & v]]
                   ((s->b s) v)))
         vec)))

(defn handlers [args]
  (let [h? '#{on on!}]
    (->> args (filter (comp h? first)) vec)))

(defmacro component [& [s & as :as args]]
  (let [args (if (list? s) args (concat (list (list 'state s)) as))]
    `(cache
      (mlet ~(bindings args)
        (mlet [handlers# (f->> (traverse ~(handlers args)) (apply merge-with concat))]
          (ossicone.effect/modify update :handlers #(merge-with concat % handlers#))
          (return {:handlers handlers#
                   :dom ~(last args)}))))))

(defmacro defc [name & args]
  `(def ~name (component ~@args)))

(defmacro defcfn [name params & args]
  `(def ~name (mlet ~(bindings args)
                (return (fn ~params ~(last args))))))
