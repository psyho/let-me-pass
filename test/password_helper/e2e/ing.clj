(ns password-helper.e2e.ing
  (:require [clojure.test :refer :all]
            [etaoin.api :as etaoin]
            [password-helper.e2e.common :as common]))


(def params {:login-url             "https://login.ingbank.pl/"
             :login-selector        {:id :login-input}
             :before-submit-login   (fn []
                                      (etaoin/wait-predicate
                                        #(not (etaoin/has-class? common/*driver* {:css "button.js-login"} :btn-disabled))))
             :submit-login-selector [{:id :js-login-form} {:tag :button}]
             :idx-from-input        common/idx-from-input-id})

(use-fixtures :each common/fixture-driver)
(deftest auto-input (common/verify-typing-input-via-helper params))
(deftest pick-chars (common/verify-typing-password-with-pick-chars params))
