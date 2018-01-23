(ns password-helper.content
  (:require [khroma.log :as console]
            [khroma.extension :as extension]
            [goog.dom :as gdom]
            [dommy.core :as dommy :refer-macros [sel1 sel]]
            [dommy.utils :refer [->Array]]
            [hipo.core :as hipo]))

(def debugging true)
(defn- debug
  "Prints a debug message if debugging is enabled"
  [message]
  (if debugging (console/log message)))

(defn password-helper-box
  "The main HTML element injected into the page"
  []
  [:div#password-helper-box
   ;; needed to hack around the fact that Chrome has broken KeyboardEvents that don't accept keyCode and which
   [:script {:type "text/javascript" :src (extension/get-url "simulate_input.js")}]
   [:input {:type "password" :placeholder "Password Helper" :id "password-helper-input"}]])

(defn inject-password-helper-box
  "Injects password helper HTML element into the current page"
  [document]
  (.appendChild (.-body document)
                (hipo/create (password-helper-box))))

(defn password-helper-stylesheet
  "Link to the extension stylesheet"
  []
  [:link {:type "text/css" :rel "stylesheet" :href (extension/get-url "password_helper.css")}])

(defn inject-stylesheet
  "Injects password helper CSS into document HEAD"
  [document]
  (.appendChild (.-head document)
                (hipo/create (password-helper-stylesheet))))

(defn inject-html
  "Injects password helper HTML and stylesheet into the page"
  [document]
  (debug "Injecting Password Helper HTML")
  (inject-stylesheet document)
  (inject-password-helper-box document))

(defn try-find
  "Tries to select the element, hides exceptions"
  [element selector]
  (try
    (sel1 element selector)
    (catch :default e
      (console/warn e)
      nil)))

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

(defn index-in-parent
  "Returns a 0 based index of element in its parent node"
  [element]
  (let [parent (dommy/parent element)
        children (->Array (dommy/children parent))]
    (.indexOf children element)))

(defn position-in-table-row
  "If input is inside a table row, returns a 1-based index of its position in that row, nil otherwise"
  [input]
  (when-let [parent-cell (find-parent input "td")]
    (str (inc (index-in-parent parent-cell)))))

(defn label-text-for-input
  "Return the label text for input based on the for attribute"
  [input document]
  (or
    (get-text (label-from-input-id input document))
    (get-text (label-nearby input))
    (position-in-table-row input)))

(defn index-for-input
  "Return the character index for editable password inputs"
  [input document]
  (let [text (label-text-for-input input document)
        digits (first (re-seq #"\d+" (str text)))
        number (js/parseInt digits)]
    (dec number)))

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

(defn build-index-input-map
  "Return a map of index => password field with password character indexes as keys"
  [document]
  (->> (find-partial-password-inputs document)
       (remove #(dommy/attr % :disabled))
       (remove #(dommy/attr % :readonly))
       (map #(vector (index-for-input % document) %))
       (into {})))

(defn input-id!
  "Returns the id of the given input, if input has no ID, then assigns a random one to it"
  [input]
  (when-not (dommy/attr input :id)
    (dommy/set-attr! input :id (random-uuid)))
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

(defn listen-for-input-changes
  "Registers event handler to listen to input changes on the password input"
  [document]
  (let [body (.-body document)
        input-map (build-index-input-map document)
        input (sel1 body :#password-helper-input)
        handler (fn [event] (on-input-change (dommy/value input) input-map))]
    (debug "Registering change handler")
    (dommy/listen! input :input handler)))

(defn maintain-input-focus
  "Don't let the password input lose focus until after user stops typing (500ms after last key press)"
  []
  (let [last-keypress-time (atom (js/Date. 0))
        input (sel1 :#password-helper-input)
        update-keypress-time #(reset! last-keypress-time (js/Date.))
        elapsed-since-last-keypress #(- (.getTime (js/Date.)) (.getTime @last-keypress-time))]
    (dommy/listen! input :keydown update-keypress-time :keyup update-keypress-time)
    (dommy/listen! input :blur #(if (< (elapsed-since-last-keypress) 500)
                                  (.focus input)))))

(defn frame-documents
  "Returns document objects for all frames found in document"
  [document]
  (letfn [(content-documents [tag] (map #(.-contentDocument %) (->Array (sel document tag))))]
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
  (when-not (sel1 :#password-helper-box)
    (debug "Injecting Password Helper")
    (inject-html document)
    (listen-for-input-changes document)
    (maintain-input-focus)))

(defn remove-password-helper
  "Removes password helper input from the page, if added"
  []
  (if-let [elem (sel1 :#password-helper-box)]
    (dommy/remove! elem)))

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

(defn init
  "Main entry point of the application. Called from content.js"
  []
  (dommy/listen! js/window :load #(if-let [document (page-contains-partial-password?)]
                                           (start-password-helper document)
                                           (wait-to-add-password-helper))))
