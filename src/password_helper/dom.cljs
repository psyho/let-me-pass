(ns password-helper.dom
  (:require [dommy.core :as dommy :refer-macros [sel1 sel]]
            [dommy.utils :refer [->Array]]
            [goog.dom :as gdom]
            [password-helper.util :as util]))


(defn frame-documents
  "Returns document objects for all frames found in document"
  [document]
  (letfn [(content-documents [tag]
            (->> (->Array (sel document tag))
                 (map (fn [frame] (util/ignore-errors #(.-contentDocument frame))))
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


(defn find-editable-partial-password-inputs
  "Scans the page for inputs that are characteristic to partial passwords"
  [root]
  (->> (find-partial-password-inputs root)
       (remove #(dommy/attr % :disabled))
       (remove #(dommy/attr % :readonly))))


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


(defn try-find
  "Tries to select the element, hides exceptions"
  [element selector]
  (util/ignore-errors
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


(defn get-text-if-number
  "Returns element text but only if the text contains a number, otherwise returns nil"
  [element]
  (util/first-number (get-text element)))


(defn label-text-for-input
  "Return the label text for input"
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
  "Return a map of password field => index"
  [document]
  (->> (find-editable-partial-password-inputs document)
       (map #(vector % (index-for-input % document)))
       (into {})))


(defn find-password-helper-root
  "Finds the password helper box in the page or returns nil"
  [document]
  (sel1 document :#password-helper-box))


(defn page-contains-partial-password?
  "Checks input on a page and returns true if the pages seems to contain a partial password"
  []
  (first (filter #(seq (find-partial-password-inputs %)) (all-frame-docs))))


