(ns password-helper.e2e.idea-bank
  (:require [clojure.test :refer :all]
            [password-helper.e2e.common :as common]))

(def params {:login-url             "https://secure.ideabank.pl/"
             :valid-login           (str (+ 100000 (rand-int 900000)))
             :login-selector        {:id :log}
             :submit-login-selector {:tag :button :type :submit :class "dalej1"}
             :idx-from-input        #(common/idx-from-input-based-on-attr % :id identity)})

(use-fixtures :each common/fixture-driver)
(deftest auto-input (common/verify-typing-input-via-helper params))
(deftest pick-chars (common/verify-typing-password-with-pick-chars params))
