(ns desmo.core
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [desmo.dom :as dom]
   [plumbing.core :refer-macros [fn->]]
   [ossicone.core :refer [mapf return traverse] :refer-macros [mlet f->]]
   [ossicone.effect :refer [env local modify run]]
   [jamesmacaulay.zelkova.signal :as signal]
   [jamesmacaulay.zelkova.time :as time]
   [cljs.core.async :refer [>! <! chan close!]]
   [cognitect.transit :as transit]
   [goog.dom :as gdom]))

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

(def state (mlet [{:keys [state path]} env]
             (return (get-in-path state path))))

(def conf (f-> env :conf))

(def send!
  (mlet [{:keys [ch path]} env]
    (return (fn [tag & values]
              (go (>! ch (apply vector tag path values)))))))

(defn on [tag f]
  (mlet [{:keys [path]} env]
    (modify update tag conj [path f])))

(defn on! [tag f!]
  (on tag (fn [s & args] (apply f! s args) s)))

(defn link [path component]
  (mlet [s state]
    (let [f (partial local component update :path conj)]
      (if (sequential? s)
        (let [path (if (map? s) :id 0)]
          (traverse (for [c s]
                      (f [path (get c path)]))))
        (f path)))))

(defn subseq? [a b] (every? true? (map = a b)))

(defn step [run {:keys [handlers state]} [tag path & args]]
  (let [new-state (->> handlers tag
                       (filter (comp (partial subseq? path) first))
                       reverse
                       (reduce (fn [s [p f]] (update-in-path s p #(apply f % args))) state))]
    (assoc (run new-state) :state new-state)))

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

(defn make-runner [component env]
  (fn [s]
    (let [{:keys [result state]} (run component :env (assoc env :state s))]
      {:tree result
       :handlers state})))

(defn run-app [component state & {:keys [conf]
                                  :or {conf {}}}]
  (let [events (signal/write-port nil)
        run (make-runner component {:ch events :path [] :conf conf})
        {:keys [handlers tree]} (run state)
        steps (->> events
                   (signal/reductions (partial step run)
                                      {:state state
                                       :handlers handlers})
                   (signal/map (fn-> (select-keys [:state :tree])))
                   to-read-port)]
    {:event (signal/map identity events)
     :state (signal/map :state steps)
     :tree (signal/map :tree steps)
     :init-tree tree}))

(defn render-app [{:keys [tree init-tree] :as m} root]
  (gdom/removeChildren root)
  (let [node (->> init-tree dom/tree (.appendChild root) atom)
        patches (->> tree
                     (sliding-pair init-tree)
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
