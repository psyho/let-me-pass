(ns password-helper.e2e.bos-bank
  (:require [clojure.test :refer :all]
            [password-helper.e2e.common :as common]))

(def params {:login-url             "https://bosbank24.pl/twojekonto"
             :login-selector        {:id :login_id}
             :submit-login-selector {:tag :img :alt "dalej" :title "dalej"}
             :idx-from-input        #(common/idx-from-input-based-on-attr % :id identity)})

(use-fixtures :each common/fixture-driver)
(deftest auto-input (common/verify-typing-input-via-helper params))
(deftest pick-chars (common/verify-typing-password-with-pick-chars params))
