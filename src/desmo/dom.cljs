(ns desmo.dom
  (:refer-clojure :exclude [time map meta])
  (:require
   [clojure.string :refer [replace]]
   [plumbing.core :refer [map-keys]]
   [cljsjs.virtual-dom])
  (:require-macros
   [desmo.dom :refer [define-tags]]))

(def tree (.-create js/virtualDom))

(def diff (.-diff js/virtualDom))

(def patch (.-patch js/virtualDom))

(defn collect-args [attrs args]
  (if (keyword? (first args))
    (collect-args (conj attrs (vec (take 2 args))) (drop 2 args))
    [attrs (flatten args)]))

(def fix-keys
  (partial map-keys (fn [k]
                (if-let [nk (k {:class "className" :for "htmlFor"})]
                  nk
                  (-> k name (replace "-" ""))))))

(defn constructor [type]
  (fn [& args]
    (let [[attrs children] (collect-args {} args)]
      (. js/virtualDom h type (-> attrs fix-keys clj->js) (clj->js children)))))

(define-tags
  a abbr address area article aside audio b base bdi bdo big blockquote body br
  button canvas caption cite code col colgroup data datalist dd del details dfn
  div dl dt em embed fieldset figcaption figure footer form h1 h2 h3 h4 h5 h6
  head header hr html i iframe img input ins kbd keygen label legend li link main
  map mark menu menuitem meta meter nav noscript object ol optgroup option output
  p param pre progress q rp rt ruby s samp script section select small source
  span strong style sub summary sup table tbody td textarea tfoot th thead time
  title tr track u ul var video wbr circle g line path polygon polyline rect svg)
