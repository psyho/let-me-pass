(ns password-helper.content
  (:require [khroma.log :as console]
            [hipo.core :as hipo]))

(defn password-helper-box
  "The main HTML element injected into the page"
  []
  [:div.password-helper-box
   [:input {:type "password" :placeholder "Password Helper" :id "password-helper-input"}]])

(defn inject-html
  "Injects password helper HTML element into the current page"
  []
  (console/log "Injecting Password Helper HTML")
  (.appendChild js/document.body
                (hipo/create (password-helper-box))))

(defn init []
  (console/log "Password Helper initialised!")
  (inject-html))
