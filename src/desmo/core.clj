(ns desmo.core
  (:require
   [ossicone.core :refer :all]))

(defn bindings [args]
  (let [s->b {'state (fn [[s]] [s `state])
              'conf (fn [[e]] [e `conf])
              'use (partial mapcat (fn [u] [u u]))}
        b? (set (keys s->b))]
    (->> args
         (filter (comp b? first))
         (mapcat (fn [[s & v]]
                   ((s->b s) v))))))

(defn handlers [args]
  (->> args (filter (comp '#{on on!} first)) vec))

(defn links [args]
  (->> args (filter (comp '#{link} first)) (mapcat rest)
       (map (fn [c]
              (let [[c & {:keys [by as] :or {by c as c}}] (if (sequential? c) c (list c))]
                {:binding [as `(link ~(keyword by) ~c)]
                 :dom [as `(map :dom ~as)]
                 :handlers `(apply merge-with concat (map :handlers ~as))})))))

(defmacro component [& [s & as :as args]]
  (let [args (if (list? s) args (concat (list (list 'state s)) as))
        ls (links args)]
    `(cache
      (mlet ~(vec (concat (bindings args) (mapcat :binding ls)))
        (mlet [hs# (f->> (traverse ~(handlers args)) (apply merge-with concat))]
          (return {:handlers (merge-with concat hs# ~@(map :handlers ls))
                   :dom (let ~(vec (mapcat :dom ls))
                          ~(last args))}))))))

(defmacro cfn [params & args]
  `(mlet ~(vec (bindings args))
     (return (fn ~params ~(last args)))))

(defmacro defc [name & args]
  `(def ~name (component ~@args)))

(defmacro defcfn [name params & args]
  `(def ~name (cfn ~params ~@args)))
