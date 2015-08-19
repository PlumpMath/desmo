(ns desmo.dom
  (:refer-clojure :exclude [time map meta])
  (:require
   [clojure.string :refer [replace]]
   [plumbing.core :refer [map-keys]]
   [plumbing.map :refer [merge-with-key]]
   [cljsjs.virtual-dom])
  (:require-macros
   [desmo.dom :refer [define-nodes]]))

(def tree (.-create js/virtualDom))

(def diff (.-diff js/virtualDom))

(def patch (.-patch js/virtualDom))

(defn props-merge [k a b]
  (case k
    :class (str a " " b)
    :default b))

(defn collect-args [props args]
  (if (keyword? (first args))
    (collect-args (merge-with-key props-merge props (apply hash-map (take 2 args)))
                  (drop 2 args))
    [props (flatten args)]))

(defrecord Node [tag props children]
  IFn
  (-invoke [this & args]
    (let [[ps cs] (collect-args props args)]
      (-> (assoc this :props ps)
          (update :children concat cs)))))

(def fix-keys
  (partial map-keys (fn [k]
                (if-let [nk (k {:class "className" :for "htmlFor"})]
                  nk
                  (-> k name (replace "-" ""))))))

(defprotocol IVdom
  (vdom [this]))

(extend-protocol IVdom
  Node
  (vdom [{:keys [tag props children]}]
    (. js/virtualDom h tag (-> props fix-keys clj->js)
       (clj->js (cljs.core/map vdom children))))
  string
  (vdom [this]
    (let [VText (.-VText js/virtualDom)]
      (VText. this))))

(define-nodes
  a address area article aside audio b base bdi bdo big blockquote body br
  button canvas caption cite code col colgroup data datalist dd del details dfn
  div dl dt em embed fieldset figcaption figure footer form h1 h2 h3 h4 h5 h6
  head header hr html i iframe img input ins kbd keygen label legend li link main
  map mark menu menuitem meta meter nav noscript object ol optgroup option output
  p param pre progress q rp rt ruby s samp script section select small source
  span strong style sub summary sup table tbody td textarea tfoot th thead time
  title tr track u ul var video wbr circle g line path polygon polyline rect svg)
