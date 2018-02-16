(ns password-helper.views
  (:require [password-helper.state :as state]
            [password-helper.actions :as actions]
            [password-helper.util :as util]))


(defn main-input-area
  "The part of the password helper box that contains the main password input"
  []

  [:div.uk-width-1-2.uk-margin-small-right
   [:div.uk-card-title.uk-h3 "Password Helper"]
   [:input.uk-input {:type        "password"
                     :placeholder "Enter your full password here"
                     :value       (state/get-password)
                     :on-change   #(actions/update-password (-> % .-target .-value))
                     :on-key-down state/update-keypress-time
                     :on-key-up   state/update-keypress-time
                     :on-blur     #(if (state/key-pressed-recently?) (.focus (.-target %)))
                     :id          "password-helper-input"}]])


(defn main-menu
  "Main menu with links to pick chars / report etc"
  []
  [:div
   [:div.uk-card-title.uk-h4 "Didn't work?"]
   [:ul.uk-list
    [:li
     [:a.uk-link-muted.password-helper-open-pick-chars {:href "#" :on-click actions/open-pick-chars} "Pick password characters >"]]
    [:li
     [:a.uk-link-muted.password-helper-open-report {:href (util/report-problem-url) :target "_blank" :on-click actions/report-problem} "Report problem >"]]]])


(defn pick-char-button-class
  "Selects a class baseed on whether the input is selected or not"
  [idx selected-letters]
  (if (contains? selected-letters idx)
    :uk-button-primary
    :uk-button-default))


(defn pick-char-button
  "Button to pick a given character password"
  [idx selected-letters]
  [:button.uk-button.uk-button-small.uk-button-default.uk-margin-small-right.uk-margin-small-bottom.password-helper-pick-chars-button
   {:key idx
    :id (str "pick-char-" idx)
    :class (pick-char-button-class idx selected-letters)
    :on-click #(actions/on-character-picked idx)}
   (str (inc idx))])


(defn pick-chars-buttons
  "Renders pick chars buttons or a message if password is empty"
  [password selected-letters]

  (if (empty? password)
    [:div.uk-text-warning "You need to type something into the password input first."]
    [:div.password-helper-grid.wrapping
     (map #(pick-char-button % selected-letters) (range (count password)))]))


(defn pick-chars
  "Pick chars UI element"
  []
  [:div.password-helper-pick-chars-container
   [:div.uk-card-title.uk-h4 "Pick Password Characters"]
   [pick-chars-buttons (state/get-password) (state/get-selected-letters)]
   [:a.uk-link-muted.password-helper-pick-chars-close {:href "#" :on-click actions/open-main-menu} "[X]"]])


(defn secondary-interaction-area
  "The part of the password helper box that contains the variable content - pick chars / report, etc"
  []

  [:div.uk-width-1-2.uk-margin-small-left
   (case (state/get-mode)
     :main-menu [main-menu]
     :pick-chars [pick-chars])])


(defn password-helper-app-root
  "This is the react root component for the password helper"
  []

  [:div.uk-card.uk-card-small.uk-card-primary.uk-card-body.uk-animation-slide-right.password-helper-grid
   [main-input-area]
   [secondary-interaction-area]])


(defn password-helper-box
  "The main HTML element injected into the page"
  []
  [:div#password-helper-box.password-helper-container

   ;; needed to hack around the fact that Chrome has broken KeyboardEvents that don't accept keyCode and which
   [:script {:type "text/javascript" :src (util/get-js-asset-url "simulate_input.js")}]

   [:div#password-helper-app-root]])


(defn password-helper-stylesheet
  "Link to the extension stylesheet"
  []
  [:link {:type "text/css" :rel "stylesheet" :href (util/get-css-asset-url "password_helper.css")}])


(defn uikit-stylesheet
  "Link to the uikit stylesheet"
  []
  [:link {:type "text/css" :rel "stylesheet" :href (util/get-css-asset-url "uikit.min.css")}])


