(ns password-helper.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [password-helper.content-test]))

(doo-tests
  'password-helper.content-test)
