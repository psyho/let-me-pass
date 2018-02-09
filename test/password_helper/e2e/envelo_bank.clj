(ns password-helper.e2e.envelo-bank
  (:require [clojure.test :refer :all]
            [password-helper.e2e.common :as common]))

(def params {:login-url             "https://online.envelobank.pl/login/main"
             :login-selector        {:id :user-alias}
             :submit-login-selector {:tag :input :type :submit :value "DALEJ"}
             :idx-from-input        #(common/idx-from-input-based-on-attr % :data-password-field-number dec)})

(use-fixtures :each common/fixture-driver)
(deftest auto-input (common/verify-typing-input-via-helper params))
(deftest pick-chars (common/verify-typing-password-with-pick-chars params))
