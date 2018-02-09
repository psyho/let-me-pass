(ns password-helper.e2e.alior-kantor
  (:require [clojure.test :refer :all]
            [password-helper.e2e.common :as common]))

(def params {:login-url             "https://kantor.aliorbank.pl/login"
             :login-selector        {:tag :input :name :login}
             :submit-login-selector [{:class :wk-submit} {:tag :input :type :submit}]
             :idx-from-input        common/idx-from-input-id})

(use-fixtures :each common/fixture-driver)
(deftest auto-input (common/verify-typing-input-via-helper params))
(deftest pick-chars (common/verify-typing-password-with-pick-chars params))
