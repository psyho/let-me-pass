(ns password-helper.content-start
  (:require [password-helper.content :as c]
            [password-helper.util :as util]))

(when util/running-inline
  (c/init)
  (c/remove-password-helper)
  (c/init-password-helper))
