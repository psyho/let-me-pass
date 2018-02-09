(ns password-helper.e2e.alior-new
  (:require [clojure.test :refer :all]
            [etaoin.api :as etaoin]
            [password-helper.e2e.common :as common]))

(def params {:login-url             "https://system.aliorbank.pl/sign-in"
             :login-selector        {:id :login}
             :valid-login           "45573286"
             :submit-login-selector {:tag :button :title "Next" :type :submit}
             :idx-from-input        common/idx-from-input-name
             :after-fill-password   (fn []
                                      (let [password-submit-btn (etaoin/query common/*driver* {:id :password-submit})
                                            disabled (etaoin/get-element-attr-el common/*driver* password-submit-btn :disabled)]
                                                    (is (nil? disabled))))})

(use-fixtures :each common/fixture-driver)
(deftest auto-input (common/verify-typing-input-via-helper params))
(deftest pick-chars (common/verify-typing-password-with-pick-chars params))
