(ns desmo.core
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [desmo.dom :as dom]
   [plumbing.core :refer-macros [fn->]]
   [ossicone.core :refer [mapf return traverse] :refer-macros [mlet f-> f->>]]
   [ossicone.effect :as eff]
   [jamesmacaulay.zelkova.signal :as signal]
   [jamesmacaulay.zelkova.time :as time]
   [cljs.core.async :refer [>! <! chan close!]]
   [cognitect.transit :as transit]
   [goog.dom :as gdom]))

(def trace (fn [_] (. js/console log (str _)) _))

(defn get-in-path [s p]
  (if-let [k (first p)]
    (if (vector? k)
      (let [[k v] k]
        (-> (filter #(= v (get % k)) s)
            first
            (get-in-path (rest p))))
      (get-in-path (get s k) (rest p)))
    s))

(defn update-in-path [s p f]
  (if-let [k (first p)]
    (if (vector? k)
      (let [[k v] k]
        (mapv #(if (= v (get % k))
                 (update-in-path % (rest p) f)
                 %)
              s))
      (assoc s k (update-in-path (get s k) (rest p) f)))
    (f s)))

(def state (f-> eff/env :state))

(def conf (f-> eff/env :conf))

(def send!
  (mlet [{:keys [ch path]} eff/env]
    (return (fn [tag & values]
              (go (>! ch (apply vector tag path values)))))))

(defn on [tag f]
  (mlet [{:keys [path]} eff/env]
    (return {tag [[path f]]})))

(defn on! [tag f!]
  (on tag (fn [s & args] (apply f! s args) s)))

(defn cache [component]
  (mlet [{:keys [path state cache]} eff/env]
    (let [cached (get cache path)]
      (if (= state (:state cached))
        (return cached)
        component))))

(defn linked [path component]
  (mlet [state (f-> eff/env :state (get-in-path [path]))
         path (f-> eff/env :path (conj path))
         {:keys [dom handlers] :as result} (eff/local component
                                                      (fn-> (assoc :path path)
                                                            (assoc :state state)))]
    (eff/modify assoc-in [:cache path] (merge {:state state} result))
    (return result)))

(defn link [path component]
  (mlet [{:keys [state]} eff/env]
    (if (sequential? state)
      (let [path (if (map? (first state)) :id 0)]
        (traverse (map #(linked [path (get % path)] component) state)))
      (mapf list (linked path component)))))

(defn log
  ([s] (log identity s))
  ([f s] (signal/map (fn [v] (. js/console log (str (f v))) v) s)))

(defn sliding-pair [init sig]
  (signal/reductions (fn [[_ old] new] [old new]) [init init] sig))

(defn to-read-port [sig]
  (let [from (signal/to-chan sig)
        to (signal/write-port nil)]
    (go-loop []
      (if-let [v (<! from)]
        (>! to v)
        (close! to))
      (recur))
    (signal/map identity to)))

(defn animate [f] (. js/window requestAnimationFrame f))

(defn subseq? [a b] (every? true? (map = a b)))

(defn step [run {:keys [handlers state cache]} [tag path & args]]
  (let [new-state (->> handlers tag
                       (filter (comp (partial subseq? path) first))
                       reverse
                       (reduce (fn [s [p f]] (update-in-path s p #(apply f % args))) state))]
    (assoc (run new-state cache) :state new-state)))

(defn make-runner [component env]
  (fn [s c] (let [{{:keys [dom handlers]} :result
                  {cache :cache} :state}
                 (eff/run component :env (assoc env :state s :cache c))]
             {:dom dom
              :handlers handlers
              :cache cache})))

(defn run-app [component state & {:keys [conf]
                                  :or {conf {}}}]
  (let [events (signal/write-port nil)
        run (make-runner component {:ch events :path [] :conf conf})
        {:keys [dom handlers cache]} (run state {})
        steps (->> events
                   (signal/reductions (partial step run)
                                      {:state state
                                       :handlers handlers
                                       :cache cache})
                   (signal/map (fn-> (select-keys [:state :dom])))
                   to-read-port)]
    {:event (signal/map identity events)
     :state (signal/map :state steps)
     :dom (signal/map :dom steps)
     :init-dom dom}))

(defn render-app [{:keys [dom init-dom] :as m} root]
  (gdom/removeChildren root)
  (let [node (->> init-dom dom/tree (.appendChild root) atom)
        patches (->> dom
                     (sliding-pair init-dom)
                     (signal/map (partial apply dom/diff))
                     signal/to-chan)]
    (go-loop []
      (reset! node (dom/patch @node (<! patches)))
      (recur)))
  m)

(defn log-app [m]
  (->> (select-keys m [:event :state])
       signal/indexed-updates
       (log (comp val first))
       signal/spawn)
  m)

(defn save-app [m store-key & {:keys [debounce path]
                               :or {debounce 0
                                    path []}}]
  (let [ch (->> m :state (time/debounce debounce) signal/to-chan)]
    (go-loop []
      (->> (get-in-path (<! ch) path)
           (transit/write (transit/writer :json))
           (. js/localStorage setItem store-key))
      (recur)))
  m)

(defn load-app [store-key]
  (transit/read (transit/reader :json)
                (. js/localStorage getItem store-key)))
