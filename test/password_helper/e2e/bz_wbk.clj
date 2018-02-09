(ns password-helper.e2e.bz-wbk
  (:require [clojure.test :refer :all]
            [password-helper.e2e.common :as common]))

(def params {:login-url             "https://www.centrum24.pl/centrum24-web/login"
             :valid-login           "12312312"
             :login-selector        {:id :input_nik}
             :submit-login-selector {:tag :input :name "loginButton"}
             :idx-from-input        common/idx-from-aria-label})

(use-fixtures :each common/fixture-driver)
(deftest auto-input (common/verify-typing-input-via-helper params))
(deftest pick-chars (common/verify-typing-password-with-pick-chars params))
