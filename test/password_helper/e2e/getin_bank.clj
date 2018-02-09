(ns password-helper.e2e.getin-bank
  (:require [clojure.test :refer :all]
            [password-helper.e2e.common :as common]))

(def params {:login-url             "https://secure.getinbank.pl"
             :valid-login           "111222"
             :login-selector        {:tag :input :name :login}
             :submit-login-selector {:tag :button :type :submit}
             :idx-from-input        #(common/idx-from-input-based-on-attr % :name identity)})

(use-fixtures :each common/fixture-driver)
(deftest auto-input (common/verify-typing-input-via-helper params))
(deftest pick-chars (common/verify-typing-password-with-pick-chars params))


