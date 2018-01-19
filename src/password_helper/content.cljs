(ns password-helper.content
  (:require [khroma.log :as console]
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
   [:input {:type "password" :placeholder "Password Helper" :id "password-helper-input"}]])

(defn inject-html
  "Injects password helper HTML element into the current page"
  [document]
  (debug "Injecting Password Helper HTML")
  (.appendChild (.-body document)
                (hipo/create (password-helper-box))))

(defn on-input-change
  "This function is called whenever the password input changes with the new password value"
  [password]
  (debug (str "New password is \"" password "\"")))

(defn listen-for-input-changes
  "Registers event handler to listen to input changes on the password input"
  [document]
  (let [body (.-body document)
        input (sel1 body :#password-helper-input)
        handler (fn [event] (on-input-change (dommy/value input)))]
    (debug "Registering change handler")
    (dommy/listen! input :keyup handler)))

(defn find-partial-password-inputs
  "Scans the page for inputs that are characteristic to partial passwords"
  [root]
  (->> (sel root :input)
       (filter #(= "password" (dommy/attr % :type)))
       (filter #(= "1" (dommy/attr % :maxlength)))))

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
