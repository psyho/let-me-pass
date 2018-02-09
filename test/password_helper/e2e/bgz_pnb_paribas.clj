(ns password-helper.e2e.bgz-pnb-paribas
  (:require [clojure.test :refer :all]
            [password-helper.e2e.common :as common]))

(def params {:login-url             "https://planet.bgzbnpparibas.pl/hades/ver/pl/demo_smart_planet/index2.html"
             :login-selector        {:class :login-input}
             :submit-login-selector {:tag :input :type :submit :class :greenButton}
             :idx-from-input        common/idx-from-position-among-other-inputs})

(use-fixtures :each common/fixture-driver)
(deftest auto-input (common/verify-typing-input-via-helper params))
(deftest pick-chars (common/verify-typing-password-with-pick-chars params))
