(ns password-helper.content
  (:require [khroma.log :as console]
            [khroma.extension :as extension]
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

(defn label-from-input-id
  "If input has an ID try to look up label for it based on the for attribute"
  [input document]
  (if-let [id (dommy/attr input :id)]
    (sel1 (.-body document) (str "label[for=" id "]"))))

(defn label-nearby
  "Finds label that is in the same parent element as the input"
  [input]
  (sel1 (dommy/parent input) :label))

(defn label-for-input
  "Return the label for input based on the for attribute"
  [input document]
  (or
    (label-from-input-id input document)
    (label-nearby input)))

(defn get-text
  "Returns element's text, handles nils gracefully"
  [elem]
  (if elem
    (dommy/text elem)
    ""))

(defn index-for-input
  "Return the character index for editable password inputs"
  [input document]
  (let [label (label-for-input input document)
        text (get-text label)
        digits (first (re-seq #"\d+" text))
        number (js/parseInt digits)]
    (dec number)))

(defn find-partial-password-inputs
  "Scans the page for inputs that are characteristic to partial passwords"
  [root]
  (->> (sel root :input)
       (filter #(= "password" (dommy/attr % :type)))
       (filter #(= "1" (dommy/attr % :maxlength)))))

(defn build-index-input-map
  "Return a map of index => password field with password character indexes as keys"
  [document]
  (->> (find-partial-password-inputs document)
       (filter #(not= "disabled" (dommy/attr % :disabled)))
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
    (listen-for-input-changes document)))

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
