(ns password-helper.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [password-helper.util-test]))

(doo-tests
  'password-helper.util-test)
