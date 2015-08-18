(ns cljs.user
  (:require
   [desmo.core
    :refer [send! on on! run-app render-app log-app save-app load-app]
    :refer-macros [defc defcfn]]
   [desmo.dom :refer [div input label p ol ul li]]
   [clojure.string :refer [blank? capitalize join split]]))

(defc term [k v]
  (use send!)
  (conf {msg :alert})
  (on :term-changed (fn [_ v] [k v]))
  (let [n (-> k str (subs 1))
        l (->> (split n "/") (map capitalize) (join " "))]
    (div :class "term"
         (label :for n l)
         (input :name n
                :value v
                :on-input #(send! :term-changed (.. % -target -value))
                :on-key-down #(case (.-which %)
                                13 (js/alert (str v " " msg))
                                8 (when (-> % .-target .-value blank?)
                                    (send! :term-removed k))
                                nil)))))

(defc terms
  (link (term :as terms))
  (on :term-removed (fn [s k] (vec (remove (comp (partial = k) first) s))))
  (div :class "terms" terms))

(defcfn term-changed [s v]
  (conf {msg :alert})
  (assoc s :log (str v " " msg)))

(defc logc s
  (p (str "log: " s)))

(defc app
  (link terms (logc :by :log :as log))
  (use term-changed)
  (on :term-changed term-changed)
  (on! :term-changed (fn [s v] (. js/console log (str "term changed: " v))))
  (div terms log))

(defn main []
  (let [store-key "app-state"
        root (. js/document getElementById "app")
        conf {:alert "THIS IS MADNESS!"}
        init-state {:terms [[:card/name "BORBO"] [:card/type "CYCLOPS"]]
                    :log "..."}
        state (or (load-app store-key) init-state)]
    (-> (run-app app state :conf conf)
        (render-app root)
        (log-app)
        (save-app store-key :debounce 1000))))

(.addEventListener js/document "DOMContentLoaded" main)
