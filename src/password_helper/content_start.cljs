(ns password-helper.content-start
  (:require [password-helper.content :as content]
            [password-helper.background :as background]
            [password-helper.util :as util]))

(when util/running-inline
  (content/init)
  (background/init)
  (content/remove-password-helper)
  (content/init-password-helper))
