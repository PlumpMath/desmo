(ns desmo.core
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [desmo.dom :as dom]
   [plumbing.core :refer-macros [fn->]]
   [ossicone.core :refer [mapf return traverse] :refer-macros [mlet]]
   [ossicone.effect :refer [env local modify run]]
   [jamesmacaulay.zelkova.signal :as signal]
   [cljs.core.async :refer [>! <! chan]]))

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

(def with-ch
  (mlet [{:keys [ch path]} env]
    (return (fn [tag & values]
              (go (>! ch (apply vector tag path values)))))))

(defn on [tag f]
  (mlet [{:keys [path]} env]
    (modify update tag conj [path f])))

(defn connect [path component]
  (mlet [s state]
    (let [f (partial local component update :path conj)]
      (if (sequential? s)
        (traverse (for [c s]
                    (f [path (get c path)])))
        (f path)))))

(defn subseq? [a b] (every? true? (map = a b)))

(defn step [run {:keys [handlers state]} [tag path & args]]
  (let [new-state (->> handlers tag
                       (filter (comp (partial subseq? path) first))
                       (reduce (fn [s [p f]] (update-in-path s p #(apply f % args))) state))]
    (assoc (run new-state) :state new-state)))

(defn log
  ([s] (log identity s))
  ([f s] (signal/map (fn [v] (. js/console log (str (f v))) v) s)))

(defn animate [f] (. js/window requestAnimationFrame f))

(defn windowed-pair [init sig]
  (signal/reductions (fn [[_ old] new] [old new]) [init init] sig))

(defn make-runner [component env]
  (fn [s]
    (let [{:keys [result state]} (run component :env (assoc env :state s))]
      {:tree result
       :handlers state})))

(defn run-app [component state root]
  (let [events (signal/write-port [:no-op])
        run (make-runner component {:ch events :path []})
        {:keys [handlers tree]} (run state)
        node (do (-> js/goog .-dom (.removeChildren root))
                 (->> tree dom/tree (.appendChild root) atom))
        patches (->> events
                     log
                     (signal/reductions (partial step run)
                                        {:state state
                                         :handlers handlers})
                     (log :state)
                     (signal/map :tree)
                     (windowed-pair tree)
                     (signal/map (partial apply dom/diff))
                     (signal/to-chan))]
    (go-loop []
      (reset! node (dom/patch @node (<! patches)))
      (recur))))
