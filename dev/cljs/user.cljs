(ns cljs.user
  (:require
   [desmo.core
    :refer [state with-ch on on! connect
            run-app render-app log-app save-app load-app]
    :refer-macros [defc on-let]]
   [desmo.dom :refer [div input label p ol ul li]]
   [clojure.string :refer [blank? capitalize join split]]))

(defc term [[k v] state
            send! with-ch]
  (on :term-changed (fn [_ v] [k v]))
  (let [n (-> k str (subs 1))
        l (->> (split n "/") (map capitalize) (join " "))]
    (div :class "term"
         (label :for n l)
         (input :name n
                :value v
                :on-input #(send! :term-changed (.. % -target -value))
                :on-key-down #(case (.-which %)
                                13 (js/alert "searching...")
                                8 (when (-> % .-target .-value blank?)
                                    (send! :term-removed k))
                                nil)))))

(defc terms [terms (connect 0 term)]
  (on :term-removed (fn [s k] (vec (remove (comp (partial = k) first) s))))
  (div :class "terms" terms))

(defc app [{log :log} state
           terms (connect :terms terms)]
  (on-let [term-changed (fn [s v] (assoc s :log v))]
    (on :term-changed term-changed)
    (on! :term-changed (fn [s v] (. js/console log (str "term changed: " v)))))
  (div
   terms
   (for [i (range 3)]
     (p (str "log: " log)))))

(defn main []
  (let [store-key "app-state"
        init-state {:terms [[:card/name "BORBO"] [:card/type "CYCLOPS"]]
                    :log "..."}
        root (. js/document getElementById "app")]
    (-> (run-app app (or (load-app store-key) init-state))
        (render-app root)
        (log-app)
        (save-app store-key :debounce 1000))))

(.addEventListener js/document "DOMContentLoaded" main)
