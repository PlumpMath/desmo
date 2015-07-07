(ns lollipop.dom
  (:refer-clojure :exclude [time map meta])
  #?(:cljs (:require-macros [lollipop.dom :refer [define-tags]])))

(do #?@(:clj
        ((defn tag-definition [tag]
           `(let [ctor# (constructor ~(name tag))]
              (defn ~tag [& args#]
                (apply ctor# args#))))

         (defmacro define-tags [& tags]
           `(do ~@(clojure.core/map tag-definition tags))))))

(do #?@(:cljs
        ((defn constructor [type]
           (fn [props & children]
             (apply js/React.createElement type (clj->js props) children)))

         (define-tags
           a abbr address area article aside audio b base bdi bdo big blockquote body br
           button canvas caption cite code col colgroup data datalist dd del details dfn
           div dl dt em embed fieldset figcaption figure footer form h1 h2 h3 h4 h5 h6
           head header hr html i iframe img input ins kbd keygen label legend li link main
           map mark menu menuitem meta meter nav noscript object ol optgroup option output
           p param pre progress q rp rt ruby s samp script section select small source
           span strong style sub summary sup table tbody td textarea tfoot th thead time
           title tr track u ul var video wbr circle g line path polygon polyline rect svg
           text))))
