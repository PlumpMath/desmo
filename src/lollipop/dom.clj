(ns lollipop.dom)

(defn tag-definition [tag]
  `(let [ctor# (constructor ~(name tag))]
     (defn ~tag [& args#]
       (apply ctor# args#))))

(defmacro define-tags [& tags]
  `(do ~@(map tag-definition tags)))
