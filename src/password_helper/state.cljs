(ns password-helper.state
  (:require [reagent.core :as r]))


(defonce app-atom (r/atom {:password           ""
                           :last-keypress-time (js/Date. 0)
                           :mode               :main-menu
                           :letter-inputs      []
                           :global-input-map   {}
                           :selected-letters   (sorted-set)}))


(defn update-state
  "Update state atom by setting key to value"
  [key value]
  (swap! app-atom assoc key value))


(defn get-input-map
  "Returns an input map, either the global one or one based on picked characters"
  ([]
    (get-input-map @app-atom))

  ([{:keys [mode global-input-map selected-letters letter-inputs]}]
   (if (= mode :pick-chars)
     (zipmap letter-inputs (concat selected-letters (repeat nil)))
     global-input-map)))


(defn get-password []
  "Returns the password typed by the user"
  (:password @app-atom))


(defn set-password [new-password]
  "Sets password in app state"
  (update-state :password new-password))


(defn set-global-input-map
  "Updates global input map (input -> idx)"
  [input-map]
  (update-state :global-input-map input-map))


(defn set-letter-inputs
  "Updates list of letter inputs"
  [inputs]
  (update-state :letter-inputs inputs))


(defn update-keypress-time
  "Updates last keypress time in tha app state atom"
  []
  (update-state :last-keypress-time (js/Date.)))


(defn get-keypress-time
  "Returns last keypress time"
  []
  (:last-keypress-time @app-atom))


(defn elapsed-since-last-keypress
  "Returns the number of milliseconds that passed since last keypress time"
  []
  (- (.getTime (js/Date.)) (.getTime (get-keypress-time))))


(defn key-pressed-recently?
  "Returns true if the user typed recently into the password input"
  []
  (< (elapsed-since-last-keypress) 500))


(defn set-mode
  "Updates app mode (main menu, pick chars, etc)"
  [mode]
  (update-state :mode mode))


(defn get-mode
  "Returns app mode"
  []
  (:mode @app-atom))


(defn toggle
  "Toggles element presence in a set"
  [set element]
  (if (contains? set element)
    (disj set element)
    (conj set element)))


(defn get-selected-letters
  "Returns a sorted set of selected letter indexes"
  []
  (:selected-letters @app-atom))


(defn toggle-selected-letter
  "Adds or removes the selected letter"
  [idx]
  (let [selected-letters (get-selected-letters)]
    (update-state :selected-letters (toggle selected-letters idx))))


