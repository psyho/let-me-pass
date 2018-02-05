(ns password-helper.content-start
  (:require [password-helper.content :as c]))

(c/remove-password-helper)
(c/init-password-helper)
(c/init)
