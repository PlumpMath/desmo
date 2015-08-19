(ns desmo.dom)

(defmacro define-nodes [& tags]
  `(do ~@(map (fn [tag]
                `(def ~tag (->Node ~(name tag) {} (list))))
              tags)))
