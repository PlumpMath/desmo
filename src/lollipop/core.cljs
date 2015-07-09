(ns lollipop.core
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [lollipop.dom :as dom :refer [div input label text]]
   [clojure.string :refer [blank? capitalize join split]]
   [plumbing.core :refer [map-vals] :refer-macros [fn-> fn->> <-]]
   [jamesmacaulay.zelkova.signal :as signal]
   [cljs.core.async :refer [>! <! chan]]
   [weasel.repl :as repl]))

(defn term [c p [k v]]
  (let [n (-> k str (subs 1))
        l (->> (split n "/") (map capitalize) (join " "))]
    {:handlers {:term-changed [[p (fn [_ v] [k v])]]}
     :markup (div :class "term" :key n
                  (label :for n
                         (text l))
                  (input :name n
                         :value v
                         :on-input #(go (>! c [:term-changed p (.. % -target -value)]))
                         :on-key-down #(case (.-which %)
                                         13 (js/alert "searching...")
                                         8 (when (-> % .-target .-value blank?)
                                             (go (>! c [:term-removed p k])))
                                         nil)))}))

(defn terms [c p ts]
  (let [terms (for [[k v] ts]
                (term c (conj p [0 k]) [k v]))]
    {:handlers (apply merge-with concat
                      {:term-removed [[p (fn [s k] (vec (remove (comp (partial = k) first) s)))]]}
                      (map :handlers terms))
     :markup (div :class "terms"
                  (map :markup terms))}))

(defn app [c p {ts :terms log :log}]
  (let [{:keys [markup handlers]} (terms c (conj p :terms) ts)]
    {:handlers (merge-with concat
                           {:term-changed [[p (fn [s v] (assoc s :log v))]]}
                           handlers)
     :markup (div :id "app"
                  markup
                  (dom/p (text (str "log: " log))))}))

(defn update-in-path [s p f]
  (if-let [k (first p)]
    (let [col (fn [df]
                (let [[k v] (df k)]
                  (mapv #(if (= v (get % k))
                           (update-in-path % (rest p) f)
                           %)
                        s)))]
      (cond
        (vector? k) (col identity)
        (map? k)    (col (fn-> seq first))
        :else       (assoc s k (update-in-path (k s) (rest p) f))))
    (f s)))

(defn subseq? [a b] (every? true? (map = a b)))

(defn step [handlers state [tag path & args]]
  (->> (handlers state)
       tag
       (filter (comp (partial subseq? path) first))
       reverse
       (reduce (fn [s [p f]] (update-in-path s p #(apply f % args))) state)))

(def log (partial signal/map (fn [_] (. js/console log (str _)) _)))

(defn render [component state root]
  (let [events (signal/write-port [:no-op])
        component (partial component events [])
        handlers (comp :handlers component)
        markup (comp :markup component)
        patches (->> events
                     log
                     (signal/reductions (partial step handlers) state)
                     log
                     (signal/map markup)
                     (signal/reductions dom/diff)
                     (signal/to-chan))
        nodes (chan)]
    (go (>! nodes (->> state markup dom/tree
                       (.appendChild root))))
    (go-loop []
      (let [patch (<! patches)
            node (<! nodes)]
        (. js/window requestAnimationFrame
           #(go (>! nodes (dom/patch node patch)))))
      (recur))))

(when-not (repl/alive?)
  (repl/connect "ws://localhost:9001"))
