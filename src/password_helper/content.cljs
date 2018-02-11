(ns password-helper.content
  (:require [reagent.core :as r]
            [dommy.core :as dommy :refer-macros [sel1 sel]]
            [hipo.core :as hipo]
            [password-helper.state :as state]
            [password-helper.views :as views]
            [password-helper.dom :as dom]
            [password-helper.util :as util]
            [password-helper.analytics :as analytics]))


(defn inject-password-helper-box
  "Injects password helper HTML element into the current page"
  [document input-map]

  (.appendChild (.-body document)
                (hipo/create (views/password-helper-box)))

  (let [app-root (sel1 document :#password-helper-app-root)]
    (state/set-global-input-map input-map)
    (state/set-letter-inputs (dom/find-editable-partial-password-inputs document))
    (r/render [views/password-helper-app-root] app-root)))


(defn inject-stylesheets
  "Injects password helper CSS into document HEAD"
  [document]
  (dom/append-to-head document (views/password-helper-stylesheet))
  (dom/append-to-head document (views/uikit-stylesheet)))


(defn inject-html
  "Injects password helper HTML and stylesheet into the page"
  [document input-map]
  (util/debug "Injecting Password Helper HTML")
  (inject-stylesheets document)
  (inject-password-helper-box document input-map))


(defn start-password-helper
  "Start Password Helper on the page (add HTML, register event handlers, etc)"
  [document]
  (when-not (dom/find-password-helper-root document)
    (util/debug "Injecting Password Helper")
    (analytics/track-event "Shown")
    (inject-html document (dom/build-index-input-map document))))


(defn remove-password-helper
  "Removes password helper input from the page, if added"
  []
  (doseq [document (dom/all-frame-docs)]
    (if-let [elem (dom/find-password-helper-root document)]
      (dommy/remove! elem))))


(defn listen-for-page-changes
  "If any change in DOM happens, checks if it seems like the page contains partial password and adds password helper in such case"
  []
  (let [mutation-observer (js/MutationObserver. (fn [_]
                                                  (if-let [document (dom/page-contains-partial-password?)]
                                                    (start-password-helper document)
                                                    (remove-password-helper))))]
    (.observe mutation-observer js/document.body #js {:childList true :subtree true})))


(defn wait-to-add-password-helper
  "Starts observing the page and adds Password Helper if necessary"
  []
  (util/debug "Partial password not found on page. Waiting to inject Password Helper until partial password shows up.")
  (listen-for-page-changes))


(defn init-password-helper
  "Initialises password helper on page"
  []
  (if-let [document (dom/page-contains-partial-password?)]
    (start-password-helper document)
    (wait-to-add-password-helper)))


(defn init
  "Main entry point of the application. Called from content.js"
  []
  (analytics/init "UA-113896134-1")
  (dommy/listen! js/window :load init-password-helper))
