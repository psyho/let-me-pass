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

(defn idx-from-input
  "Returns the index corresponding to the password input"
  [input]
  (let [id (get-element-attr-el *driver* input :id)
        number (re-find #"\d+" id)]
    (dec (Integer/parseInt number))))

(deftest ing
  (go *driver* "https://login.ingbank.pl/")
  (wait-visible *driver* {:id :login-input})
  (fill *driver* {:id :login-input} "123123123")
  (wait-predicate #(not (has-class? *driver* {:css "button.js-login"} :btn-disabled)))
  (click *driver* [{:id :js-login-form} {:tag :button}])
  (wait-visible *driver* {:id :password-helper-input})
  (fill *driver* {:id :password-helper-input} password)
  (let [password-inputs (query-all *driver* {:tag :input :type :password :maxlength 1})]
    (doseq [input password-inputs]
      (let [idx (idx-from-input input)
            char (str (nth password idx))]
        (is (= char (get-element-value-el *driver* input)))))))

