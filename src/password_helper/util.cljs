(ns password-helper.util
  (:require [khroma.log :as console]
            [khroma.extension :as extension]
            [cljs.core.async :as async :refer [>! go]]))


(def debugging true)
(defn- debug
  "Prints a debug message if debugging is enabled"
  [message & args]
  (if debugging (apply console/log message args)))


(defn ignore-errors
  "Executes the given function and ignores errors thrown (logs them to console)"
  [f]
  (try
    (f)
    (catch :default e
      (debug e)
      nil)))


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


(defn make-incoming-channel []
  (let [channel (async/chan)]
    (js/chrome.runtime.onMessage.addListener (fn [request _ _]
                    (go (>! channel (js->clj request)))))
    channel))


(defn make-outgoing-channel []
  (let [channel (async/chan)]
    (async/go-loop []
                   (let [message (<! channel)]
                     (chrome-send-message message)
                     (recur)))
    channel))


(def communication-channel (async/chan))
(def chrome-outgoing-channel (make-outgoing-channel))
(def chrome-incoming-channel (make-incoming-channel))

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


(def prefilled-report-problem-url "https://docs.google.com/forms/d/e/1FAIpQLSdC_SMeYaVpx-60rxR6XXhDiDHoJLltJNXC3MAmxS8PgMbvBw/viewform?usp=pp_url&entry.1715275636=URL.GOES.HERE&entry.938365311&entry.1821262036")


(defn report-problem-url
  "Returns the URL for report problem form"
  []
  (.replace prefilled-report-problem-url "URL.GOES.HERE" (current-hostname)))
