(ns password-helper.content-start
  (:require [password-helper.content :as c]))

(c/init)
(c/remove-password-helper)
(c/init-password-helper)
