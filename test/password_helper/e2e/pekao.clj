(ns password-helper.e2e.pekao
  (:require [clojure.test :refer :all]
            [password-helper.e2e.common :as common]))

(def params {:login-url             "http://demo.pekao24.pl/"
             :login-selector        {:id :parUsername}
             :submit-login-selector {:id :butLogin}
             :idx-from-input        #(common/idx-from-input-based-on-attr % :id identity)})

(use-fixtures :each common/fixture-driver)
(deftest auto-input (common/verify-typing-input-via-helper params))
(deftest pick-chars (common/verify-typing-password-with-pick-chars params))
