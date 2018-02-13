(ns password-helper.background
  (:require [password-helper.analytics :as analytics]
            [password-helper.util :as util]
            [khroma.runtime :as runtime]
            [cljs.core.async :refer [go <!]]))

(defn handle-message
  [{type "type" data "data" :as message}]
  (util/debug "Message from content script:" message)
  (case type
    "send-analytics" (apply analytics/gtag data)
    (util/debug "Unknown message: " message)))


(defn listen-for-messages
  "Listens for messages sent from the content script"
  []
  (go (let [conns (runtime/on-connect)
            content (<! conns)]
        (handle-message (<! content))
        (listen-for-messages))))


(defn init
  "Initialises the background page"
  []
  (analytics/init "UA-113896134-1")
  (listen-for-messages))
