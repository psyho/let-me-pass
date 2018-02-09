(ns password-helper.e2e.tmobile-bank
  (:require [clojure.test :refer :all]
            [password-helper.e2e.common :as common]))

(def params {:login-url             "https://system.t-mobilebankowe.pl/web/login"
             :valid-login           "123123"
             :login-selector        {:tag :input :type :text :maxlength 100}
             :submit-login-selector [{:class "RjVxsd"} {:tag :button}]
             :idx-from-input        common/idx-from-sibling-label})

(use-fixtures :each common/fixture-driver)
(deftest auto-input (common/verify-typing-input-via-helper params))
(deftest pick-chars (common/verify-typing-password-with-pick-chars params))

