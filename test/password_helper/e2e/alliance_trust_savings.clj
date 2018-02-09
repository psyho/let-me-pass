(ns password-helper.e2e.alliance-trust-savings
  (:require [clojure.test :refer :all]
            [password-helper.e2e.common :as common]))

(def params {:login-url             "https://atonline.alliancetrust.co.uk/atonline/login.jsp"
             :valid-login           "123123"
             :login-selector        {:id :pinId}
             :submit-login-selector [{:tag :form :name :login} {:tag :input :type :submit}]
             :idx-from-input        common/idx-from-parent-text})

(use-fixtures :each common/fixture-driver)
(deftest auto-input (common/verify-typing-input-via-helper params))
(deftest pick-chars (common/verify-typing-password-with-pick-chars params))

