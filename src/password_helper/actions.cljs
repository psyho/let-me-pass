(ns password-helper.actions
  (:require [password-helper.state :as state]
            [password-helper.dom :as dom]
            [password-helper.util :as util]))


(defn on-input-change
  "This function is called whenever the password input changes with the new password value"
  ([]
   (on-input-change (state/get-password) (state/get-input-map)))

  ([password input-map]
   (util/debug (str "Password changed to " \" password \"))
   (doseq [[input idx] input-map]
     (dom/simulate-user-input input (get password idx "")))))


(defn update-password
  "Updates password in the app state atom and triggers callback"
  [new-password]
  (state/set-password new-password)
  (on-input-change))


(defn open-pick-chars
  "Opens pick characters menu"
  [evt]
  (state/set-mode :pick-chars)
  (on-input-change)
  (.preventDefault evt))


(defn open-main-menu
  "Opens main menu"
  [evt]
  (state/set-mode :main-menu)
  (on-input-change)
  (.preventDefault evt))


(defn on-character-picked
  "Toggles the character selection and updates the password"
  [idx]
  (state/toggle-selected-letter idx)
  (on-input-change))


