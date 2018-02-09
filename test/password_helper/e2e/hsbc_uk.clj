(ns password-helper.e2e.hsbc-uk
  (:require [clojure.test :refer :all]
            [password-helper.e2e.common :as common]
            [etaoin.api :as etaoin]))

(defn hsbc-adjust
  "In HSBC, input 8 is last character and input 7 is second last"
  [password number]
  (condp = number
    7 (- (count password) 2)
    8 (- (count password) 1)
    (dec number)))

(def params {:login-url             "https://www.hsbc.co.uk/1/2/welcome-gsp?initialAccess=true&IDV_URL=hsbc.MyHSBC_pib"
             :valid-login           "John123"
             :login-selector        {:id "Username1"}
             :submit-login-selector {:tag :input :type :submit :class :submit_input}
             :before-fill-password  (fn []
                                      (etaoin/wait-visible common/*driver* {:css ".toggleButtons"})
                                      (etaoin/click common/*driver* [{:css ".toggleButtons"} {:tag :li :aria-checked :false}]))
             :idx-from-input        #(common/idx-from-input-based-on-attr % :id (partial hsbc-adjust common/password))})

(use-fixtures :each common/fixture-driver)
(deftest auto-input (common/verify-typing-input-via-helper params))
(deftest pick-chars (common/verify-typing-password-with-pick-chars params))
