(ns password-helper.content
  (:require [khroma.log :as console]
            [dommy.core :as dommy :refer-macros [sel1]]
            [hipo.core :as hipo]))

(def debugging true)
(defn- debug
  "Prints a debug message if debugging is enabled"
  [message]
  (if debugging (console/log message)))

(defn password-helper-box
  "The main HTML element injected into the page"
  []
  [:div.password-helper-box
   [:input {:type "password" :placeholder "Password Helper" :id "password-helper-input"}]])

(defn inject-html
  "Injects password helper HTML element into the current page"
  []
  (debug "Injecting Password Helper HTML")
  (.appendChild js/document.body
                (hipo/create (password-helper-box))))

(defn on-input-change
  "This function is called whenever the password input changes with the new password value"
  [password]
  (debug (str "New password is \"" password "\"")))

(defn listen-for-input-changes
  "Registers event handler to listen to input changes on the password input"
  []
  (let [input (sel1 :#password-helper-input)
        handler (fn [event] (on-input-change (dommy/value input)))]
    (debug "Registering change handler")
    (dommy/listen! input :keyup handler)))

(defn init []
  (debug "Password Helper initialised!")
  (inject-html)
  (listen-for-input-changes))
