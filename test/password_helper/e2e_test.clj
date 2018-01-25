(ns password-helper.e2e-test
  (:require [clojure.test :refer :all]
            [etaoin.api :refer :all]))

(def ^:dynamic *driver*)

(def project-dir (System/getProperty "user.dir"))

(def password "0123456789abcdefghijklmnopqrstuvwxyz")

(defn fixture-driver
  "Executes a test running a driver. Binds a driver
   with the global *driver* variable."
  [f]
  (with-chrome {:args [(str "--load-extension=" project-dir "/target/unpacked")]} driver
               (binding [*driver* driver]
                 (f))))

(use-fixtures
  :each ;; start and stop driver for each test
  fixture-driver)

(deftest ing
  (go *driver* "https://login.ingbank.pl/")
  (wait-visible *driver* {:id :login-input})
  (fill *driver* {:id :login-input} "123123123")
  (wait-predicate #(not (has-class? *driver* {:css "button.js-login"} :btn-disabled)))
  (click *driver* [{:id :js-login-form} {:tag :button}])
  (wait-visible *driver* {:id :password-helper-input})
  (fill *driver* {:id :password-helper-input} password)
  (let [idx (first (filter #(visible? *driver* {:id (str "mask-" (inc %))}) (range)))
        input-id (str "mask-" (inc idx))
        char (str (nth password idx))]
    (is (= char (get-element-value *driver* {:id input-id})))
    ))

