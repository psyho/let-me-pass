(ns password-helper.analytics
  (:require [password-helper.dom :as dom]
            [password-helper.util :as util]
            [khroma.runtime :as runtime]
            [cljs.core.async :refer [go >!]]))


(defn google-analytics-script
  "Script element for google analytics"
  [tracking-code]
  [:script {:async true :src (str "https://www.googletagmanager.com/gtag/js?id=" tracking-code)}])


(def gtag-native (js* "function(){dataLayer.push(arguments);}"))


(defn gtag
  "Function to communicate with google analytics"
  [& args]
  (apply util/debug "gtag" args)
  (apply gtag-native (clj->js args)))


(defn track-event
  "Track event with google analytics"
  ([name]
    (track-event name {}))

  ([name {:keys [category label value] :or {category :engagement label (util/current-hostname)}}]
   (let [bg (util/get-outgoing-channel)]
     (go (>! bg {"type" "send-analytics"
                 "data" ["event" name {"event_category" category "event_label" label "value" value}]})))))


(defn install-script
  "Installs the google analytics script element on the page"
  [tracking-code]
  (dom/append-to-head js/document (google-analytics-script tracking-code)))


(defn init
  "Initialise google analytics on page"
  [tracking-code]
  (install-script tracking-code)
  (when (undefined? (.-dataLayer js/window))
    (set! (.-dataLayer js/window) #js []))
  (gtag :js (js/Date.))
  (gtag :config tracking-code {:send_page_view false}))
