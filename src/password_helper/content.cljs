(ns password-helper.content
  (:require [khroma.log :as console]
            [khroma.extension :as extension]
            [goog.dom :as gdom]
            [reagent.core :as r]
            [dommy.core :as dommy :refer-macros [sel1 sel]]
            [dommy.utils :refer [->Array]]
            [hipo.core :as hipo]))

(def debugging true)
(defn- debug
  "Prints a debug message if debugging is enabled"
  [message]
  (if debugging (console/log message)))

(def running-inline (undefined? js/chrome.extension))

(defn get-asset-url
  "Returns asset URL"
  [type name]
  (if running-inline
    (str "../resources/" type  "/" name)
    (extension/get-url name)))

(defn get-css-asset-url
  "Returns CSS asset URL"
  [name]
  (get-asset-url "css" name))

(defn get-js-asset-url
  "Returns JS asset URL"
  [name]
  (get-asset-url "js" name))

(defn input-id!
  "Returns the id of the given input, if input has no ID, then assigns a random one to it"
  [input]
  (when-not (dommy/attr input :id)
    (dommy/set-attr! input :id (str "pass-" (random-uuid))))
  (dommy/attr input :id))


(defn simulate-user-input
  "Simulates user typing into an input"
  [input value]
  ;; see simulate_input.js for the part that listens to this event and triggers actual events on the input
  (let [event (js/CustomEvent. "simulate-input" #js {:bubbles true
                                                     :cancelable true
                                                     :detail #js {:id (input-id! input)
                                                                  :value value}})]
    (.dispatchEvent input event)))

(defn on-input-change
  "This function is called whenever the password input changes with the new password value"
  [password input-map]
  (debug (str "Password changed to " \" password \"))
  (doseq [[idx input] input-map]
    (simulate-user-input input (get password idx ""))))


(defonce app-atom (r/atom {:password ""
                           :last-keypress-time (js/Date. 0)}))


(defn update-password
  "Updates password in the app state atom and triggers callback"
  [new-password input-map]
  (swap! app-atom assoc :password new-password)
  (on-input-change (:password @app-atom) input-map))


(defn update-keypress-time
  "Updates last keypress time in tha app state atom"
  []
  (swap! app-atom assoc :last-keypress-time (js/Date.)))


(defn elapsed-since-last-keypress
  "Returns the number of milliseconds that passed since last keypress time"
  []
  (- (.getTime (js/Date.)) (.getTime (:last-keypress-time @app-atom))))


(defn key-pressed-recently?
  "Returns true if the user typed recently into the password input"
  []
  (< (elapsed-since-last-keypress) 500))

(defn main-input-area
  "The part of the password helper box that contains the main password input"
  [input-map]

  [:div.uk-width-1-2.uk-margin-small-right
   [:div.uk-card-title.uk-h3 "Password Helper"]
   [:input.uk-input {:type "password"
                     :placeholder "Enter your full password here"
                     :value (:password @app-atom)
                     :on-change #(update-password (-> % .-target .-value) input-map)
                     :on-key-down update-keypress-time
                     :on-key-up update-keypress-time
                     :on-blur #(if (key-pressed-recently?) (.focus (.-target %)))
                     :id "password-helper-input"}]])

(defn secondary-interaction-area
  "The part of the password helper box that contains the variable content - pick chars / report, etc"
  []

  [:div.uk-width-1-2.uk-margin-small-left
   [:div.uk-card-title.uk-h4 "Didn't work?"]
   [:ul.uk-list
    [:li
     [:a.uk-link-muted {:href "#" :on-click #()} "Pick password characters >"]]
    [:li
     [:a.uk-link-muted {:href "#" :on-click #()} "Report unsupported page >"]]]])

(defn password-helper-app-root
  "This is the react root component for the password helper"
  [input-map]

  [:div.uk-card.uk-card-small.uk-card-primary.uk-card-body.uk-animation-slide-right.password-helper-grid
   [main-input-area input-map]
   [secondary-interaction-area]])


(defn password-helper-box
  "The main HTML element injected into the page"
  []
  [:div#password-helper-box.password-helper-container

   ;; needed to hack around the fact that Chrome has broken KeyboardEvents that don't accept keyCode and which
   [:script {:type "text/javascript" :src (get-js-asset-url "simulate_input.js")}]

   [:div#password-helper-app-root]])

(defn inject-password-helper-box
  "Injects password helper HTML element into the current page"
  [document input-map]
  (.appendChild (.-body document)
                (hipo/create (password-helper-box)))
  (let [app-root (sel1 document :#password-helper-app-root)]
    (r/render [password-helper-app-root input-map] app-root)))

(defn password-helper-stylesheet
  "Link to the extension stylesheet"
  []
  [:link {:type "text/css" :rel "stylesheet" :href (get-css-asset-url "password_helper.css")}])

(defn uikit-stylesheet
  "Link to the uikit stylesheet"
  []
  [:link {:type "text/css" :rel "stylesheet" :href (get-css-asset-url "uikit.min.css")}])

(defn append-stylesheet [document html]
  "Append stylesheet tag to head element"
  (.appendChild (.-head document) (hipo/create html)))

(defn inject-stylesheets
  "Injects password helper CSS into document HEAD"
  [document]
  (append-stylesheet document (password-helper-stylesheet))
  (append-stylesheet document (uikit-stylesheet)))

(defn inject-html
  "Injects password helper HTML and stylesheet into the page"
  [document input-map]
  (debug "Injecting Password Helper HTML")
  (inject-stylesheets document)
  (inject-password-helper-box document input-map))

(defn ignore-errors
  "Executes the given function and ignores errors thrown (logs them to console)"
  [f]
  (try
    (f)
    (catch :default e
      (console/warn e)
      nil)))

(defn try-find
  "Tries to select the element, hides exceptions"
  [element selector]
  (ignore-errors
    #(sel1 element selector)))

(defn label-from-aria-label
  "Returns the label element based on the aria-labelledby attribute of the input"
  [input document]
  (if-let [label-id (dommy/attr input :aria-labelledby)]
    (try-find (.-body document) (str "#" label-id))))

(defn label-from-input-id
  "If input has an ID try to look up label for it based on the for attribute"
  [input document]
  (if-let [id (dommy/attr input :id)]
    (try-find (.-body document) (str "label[for=" id "]"))))

(defn label-nearby
  "Finds label that is in the same parent element as the input"
  [input]
  (sel1 (dommy/parent input) :label))

(defn get-text
  "Returns element's text, handles nils gracefully"
  [elem]
  (if elem
    (dommy/text elem)))

(defn find-parent
  "Recursively traverses the DOM to find a parent matching the given tag name"
  [element tag-name]
  (gdom/getAncestorByTagNameAndClass element tag-name))

(defn partial-password-input?
  "Checks if element looks like a partial password input"
  [element]
  (and
    (= "password" (dommy/attr element :type))
    (= "1" (dommy/attr element :maxlength))))

(defn find-partial-password-inputs
  "Scans the page for inputs that are characteristic to partial passwords"
  [root]
  (->> (sel root :input)
       (filter partial-password-input?)))

(defn index-in-parent
  "Returns a 0 based index of element in its parent node (includes only nodes of the same type)"
  [element]
  (let [parent (dommy/parent element)
        children (->Array (dommy/children parent))
        children-of-same-type (filter #(= (.-tagName element) (.-tagName %)) children)]
    (.indexOf children-of-same-type element)))

(defn position-in-table-row
  "If input is inside a table row, returns a 1-based index of its position in that row, nil otherwise"
  [input]
  (when-let [parent-cell (find-parent input "td")]
    (str (inc (index-in-parent parent-cell)))))

(defn deepest-individual-ancestor
  "Returns the most nested ancestor that does not contain multiple partial password inputs"
  [element]
  (if-let [parent (dommy/parent element)]
    (let [inputs (find-partial-password-inputs parent)]
      (if (< 1 (count inputs))
        element
        (recur parent)))))

(defn first-number
  "Returns first number found in text or nil"
  [text]
  (cond
    (re-find #"(?i)second.+last" (str text)) "-1"
    (re-find #"(?i)last" (str text)) "0"
    :else (first (re-seq #"\d+" (str text)))))

(defn get-text-if-number
  "Returns element text but only if the text contains a number, otherwise returns nil"
  [element]
  (first-number (get-text element)))

(defn label-text-for-input
  "Return the label text for input based on the for attribute"
  [input document]
  (or
    (get-text-if-number (label-from-aria-label input document))
    (get-text-if-number (label-from-input-id input document))
    (get-text-if-number (label-nearby input))
    (get-text-if-number (deepest-individual-ancestor input))
    (position-in-table-row input)))

(defn index-for-input
  "Return the character index for editable password inputs"
  [input document]
  (let [digits (label-text-for-input input document)
        number (js/parseInt digits)]
    (dec number)))

(defn build-index-input-map
  "Return a map of index => password field with password character indexes as keys"
  [document]
  (->> (find-partial-password-inputs document)
       (remove #(dommy/attr % :disabled))
       (remove #(dommy/attr % :readonly))
       (map #(vector (index-for-input % document) %))
       (into {})))

(defn at-index
  "Returns character at index or empty string"
  [string idx]
  (let [positive-idx (if (< idx 0)
                       (+ (count string) idx)
                       idx)]
    (get string positive-idx "")))


(defn find-password-helper-root
  "Finds the password helper box in the page or returns nil"
  [document]
  (sel1 document :#password-helper-box))

(defn frame-documents
  "Returns document objects for all frames found in document"
  [document]
  (letfn [(content-documents [tag]
            (->> (->Array (sel document tag))
                 (map (fn [frame] (ignore-errors #(.-contentDocument frame))))
                 (remove nil?)))]
    (concat
      (content-documents :frame)
      (content-documents :iframe))))

(defn all-frame-docs
  "Returns a collection of all frame documents on the page (recursively) including the top-level document"
  ([]
   (all-frame-docs [js/document]))

  ([documents]
   (if (seq documents)
     (let [first-doc (first documents)]
       (lazy-seq (cons first-doc (all-frame-docs (concat (rest documents) (frame-documents first-doc))))))
     (lazy-seq))))

(defn page-contains-partial-password?
  "Checks input on a page and returns true if the pages seems to contain a partial password"
  []
  (first (filter #(seq (find-partial-password-inputs %)) (all-frame-docs))))

(defn start-password-helper
  "Start Password Helper on the page (add HTML, register event handlers, etc)"
  [document]
  (when-not (find-password-helper-root document)
    (debug "Injecting Password Helper")
    (inject-html document (build-index-input-map document))))

(defn remove-password-helper
  "Removes password helper input from the page, if added"
  []
  (doseq [document (all-frame-docs)]
    (if-let [elem (find-password-helper-root document)]
      (dommy/remove! elem))))

(defn listen-for-page-changes
  "If any change in DOM happens, checks if it seems like the page contains partial password and adds password helper in such case"
  []
  (let [mutation-observer (js/MutationObserver. (fn [_]
                                                  (if-let [document (page-contains-partial-password?)]
                                                    (start-password-helper document)
                                                    (remove-password-helper))))]
    (.observe mutation-observer js/document.body #js {:childList true :subtree true})))

(defn wait-to-add-password-helper
  "Starts observing the page and adds Password Helper if necessary"
  []
  (debug "Partial password not found on page. Waiting to inject Password Helper until partial password shows up.")
  (listen-for-page-changes))

(defn init-password-helper
  "Initialises password helper on page"
  []
  (if-let [document (page-contains-partial-password?)]
    (start-password-helper document)
    (wait-to-add-password-helper)))

(defn init
  "Main entry point of the application. Called from content.js"
  []
  (dommy/listen! js/window :load init-password-helper))
