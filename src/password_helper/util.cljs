(ns password-helper.util
  (:require [cljs.core.async :as async :refer [>! go]]))


(def debugging true)
(defn- debug
  "Prints a debug message if debugging is enabled"
  [message & args]
  (if debugging (apply js/console.log message args)))


(defn ignore-errors
  "Executes the given function and ignores errors thrown (logs them to console)"
  [f]
  (try
    (f)
    (catch :default e
      (debug e)
      nil)))


(def running-inline
  "Flag indicating whether the app is running
  inline - loaded directly into the page
  not inline - as a chrome extension"
  (undefined? js/chrome.extension))


(def translations
  "Translations map used in inline mode"
  {:description "Automatically fills passwords on sites that ask for random password characters for you."
   :input-placeholder "Enter your full password here"
   :main-menu-header "Didn't work?"
   :pick-password-chars-action "Pick password characters >"
   :report-problem-action "Report problem >"
   :pick-chars-warning "You need to type something into the password input first."
   :pick-chars-header "Pick Password Characters"})


(defn t
  "Gets i18n message by name"
  [key]
  (if running-inline
    (get translations key (str "Key " (name key) " not found!"))
    (js/chrome.i18n.getMessage (clojure.string/replace (name key) "-" "_"))))


(defn get-asset-url
  "Returns asset URL"
  [type name]
  (if running-inline
    (str "../resources/" type  "/" name)
    (js/chrome.extension.getURL name)))


(defn get-css-asset-url
  "Returns CSS asset URL"
  [name]
  (get-asset-url "css" name))


(defn get-js-asset-url
  "Returns JS asset URL"
  [name]
  (get-asset-url "js" name))


(defn first-number
  "Returns first number found in text or nil"
  [text]
  (cond
    (re-find #"(?i)second.+last" (str text)) "-1"
    (re-find #"(?i)last" (str text)) "0"
    :else (first (re-seq #"\d+" (str text)))))


(defn at-index
  "Returns character at index or empty string"
  [string idx]
  (let [positive-idx (if (< idx 0)
                       (+ (count string) idx)
                       idx)]
    (get string positive-idx "")))


(defn current-hostname
  "Returns the hostname of the current page"
  []
  (-> js/window .-location .-hostname))


(defn chrome-send-message
  "Sends a message from content script to background page
  using chrome apis"
  [message]
  (js/chrome.runtime.sendMessage (clj->js message)))


(defn make-incoming-channel
  "Returns a channel that can be used to listen to messages incoming from the content script"
  []
  (let [channel (async/chan)]
    (js/chrome.runtime.onMessage.addListener (fn [request _ _]
                    (go (>! channel (js->clj request)))))
    channel))


(defn make-outgoing-channel
  "Returns a channel that can be used to send messages from the content script to the background page"
  []
  (let [channel (async/chan)]
    (async/go-loop []
                   (let [message (<! channel)]
                     (chrome-send-message message)
                     (recur)))
    channel))


(def communication-channel
  "Channel used in inline mode for two-way communication between 'background page' and 'content script'"
  (async/chan))


(def chrome-outgoing-channel
  "Channel used to send messages from content script to background page"
  (when-not running-inline (make-outgoing-channel)))


(def chrome-incoming-channel
  "Channel used to read messages coming from the content script in the background page"
  (when-not running-inline (make-incoming-channel)))


(defn get-incoming-channel
  "Returns a channel which can be used to read messages sent from content script"
  []
  (if running-inline
    communication-channel
    chrome-incoming-channel))


(defn get-outgoing-channel
  "Returns a channel which can be used to send messages to the background script"
  []
  (if running-inline
    communication-channel
    chrome-outgoing-channel))


(def prefilled-report-problem-url
  "https://docs.google.com/forms/d/e/1FAIpQLSdC_SMeYaVpx-60rxR6XXhDiDHoJLltJNXC3MAmxS8PgMbvBw/viewform?usp=pp_url&entry.1715275636=URL.GOES.HERE&entry.938365311&entry.1821262036")


(defn report-problem-url
  "Returns the URL for report problem form"
  []
  (.replace prefilled-report-problem-url "URL.GOES.HERE" (current-hostname)))
