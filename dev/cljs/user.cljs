(ns cljs.user
  (:require
   [desmo.core
    :refer [state with-ch on connect run-app]
    :refer-macros [defc]]
   [desmo.dom :refer [div input label p]]
   [clojure.string :refer [blank? capitalize join split]]
   [weasel.repl :as repl]))

(when-not (repl/alive?)
  (repl/connect "ws://localhost:9001"))

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
  (on :term-changed (fn [s v] (assoc s :log v)))
  (div
   terms
   (p (str "log: " log))))

(def initial-state {:terms [[:card/name "BORBO"] [:card/type "CYCLOPS"]]
                    :log "..."})

(def root #(. js/document getElementById "app"))

(comment
  (run-app app initial-state (root)))
