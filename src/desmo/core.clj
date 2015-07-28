(ns desmo.core
  (:require
   [ossicone.core :refer :all]))

(defmacro on-let [bindings & body]
  `(let ~bindings (mdo ~@body)))

(defn collect-bindings [args & {ks :keys}]
  (let [k->b {:state (fn [s] [s `state])
              :conf (fn [e] [e `conf])
              :use (partial mapcat (fn [u] [u u]))
              :link (partial mapcat (fn [c] [c `(link ~(keyword c) ~c)]))}
        k->b (if ks (select-keys k->b ks) k->b)]
    (->> args
         (partition-all 2)
         (reduce (fn [m [k v :as p]]
                   (if (keyword? k)
                     (update m :bindings concat (if-let [f (k k->b)] (f v) []))
                     (update m :body concat p)))
                 {})
         (mapf vec))))

(defmacro component [& args]
  (let [args (let [s (first args)]
               (if (keyword? s) args (concat (list :state s) (rest args))))
        {:keys [bindings body]} (collect-bindings args)
        body (concat (butlast body) (list `(return ~(last body))))]
    `(mlet ~bindings ~@body)))

(defmacro defc [name & args]
  `(def ~name (component ~@args)))

(defmacro defcfn [name params & args]
  (let [{:keys [bindings body]} (collect-bindings args :keys [:use :conf])]
    `(def ~name (mlet ~bindings
                  (return (fn ~params ~@body))))))
