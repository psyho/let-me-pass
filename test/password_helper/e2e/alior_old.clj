(ns password-helper.e2e.alior-old
  (:require [clojure.test :refer :all]
            [etaoin.api :as etaoin]
            [password-helper.e2e.common :as common]))

(def params {:login-url             "https://aliorbank.pl/hades/do/Login"
             :login-selector        {:id :inputContent}
             :submit-login-selector [{:id :buttonTr} {:tag :a}]
             :before-fill-password  (fn []
                                      (etaoin/wait-visible common/*driver* {:tag :frameset})
                                      (etaoin/switch-frame common/*driver* {:tag :frame :name :main}))
             :idx-from-input        common/idx-from-input-id})

(use-fixtures :each common/fixture-driver)
(deftest auto-input (common/verify-typing-input-via-helper params))
(deftest pick-chars (common/verify-typing-password-with-pick-chars params))
