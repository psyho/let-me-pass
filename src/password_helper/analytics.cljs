(ns password-helper.analytics
  (:require [password-helper.dom :as dom]
            [password-helper.util :as util]
            [cljs.core.async :refer [go >!]]))


(defn google-analytics-script
  "Script element for google analytics"
  []
  [:script {:async true :src "https://www.google-analytics.com/analytics.js"}])


(defn define-ga []
  (js* "window.ga=window.ga||function(){(ga.q=ga.q||[]).push(arguments)};ga.l=+new Date;"))


(defn ga
  "Function to communicate with google analytics"
  [& args]
  (apply util/debug "ga" args)
  (apply js/ga (clj->js args)))


(defn track-event
  "Track event with google analytics"
  ([name]
    (track-event name {}))

  ([name {:keys [category label value] :or {category :engagement label (util/current-hostname)}}]
   (let [bg (util/get-outgoing-channel)]
     (go (>! bg {"type" "send-analytics"
                 "data" ["send" "event" category name label value]})))))


(defn install-script
  "Installs the google analytics script element on the page"
  []
  (dom/append-to-head js/document (google-analytics-script)))


(defn init
  "Initialise google analytics on page"
  [tracking-code]
  (install-script)
  (define-ga)
  (ga "create" tracking-code "auto")
  (ga "set" "checkProtocolTask" #()))
