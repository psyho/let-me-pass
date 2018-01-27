(ns password-helper.e2e-test
  (:require [clojure.test :refer :all]
            [etaoin.api :refer :all]))

(def ^:dynamic *driver*)

(def project-dir (System/getProperty "user.dir"))

(def password "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")

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

(defn idx-from-input-id
  "Returns the index corresponding to the password input"
  [input]
  (let [id (get-element-attr-el *driver* input :id)
        number (re-find #"\d+" id)]
    (dec (Integer/parseInt number))))

(defn verify-typing-input-via-helper [{:keys [login-url
                                              login-selector
                                              valid-login
                                              before-submit-login
                                              submit-login-selector
                                              before-fill-password
                                              idx-from-input]
                                       :or {valid-login "123123123"
                                            before-submit-login #()
                                            before-fill-password #()}}]
  (go *driver* login-url)
  (wait-visible *driver* login-selector)
  (fill *driver* login-selector valid-login)
  (before-submit-login)
  (click *driver* submit-login-selector)
  (before-fill-password)
  (wait-visible *driver* {:id :password-helper-input})
  (fill *driver* {:id :password-helper-input} password)
  (let [password-inputs (query-all *driver* {:tag :input :type :password :maxlength 1})
        password-inputs (remove #(get-element-attr-el *driver* % :readonly) password-inputs)]
    (doseq [input password-inputs]
      (let [idx (idx-from-input input)
            char (str (nth password idx))]
        (is (= char (get-element-value-el *driver* input)))))))

(deftest ing
  (verify-typing-input-via-helper {:login-url "https://login.ingbank.pl/"
                                   :login-selector {:id :login-input}
                                   :before-submit-login (fn []
                                                          (wait-predicate
                                                            #(not (has-class? *driver* {:css "button.js-login"} :btn-disabled))))
                                   :submit-login-selector [{:id :js-login-form} {:tag :button}]
                                   :idx-from-input idx-from-input-id}))

(deftest alior-old
  (verify-typing-input-via-helper {:login-url "https://aliorbank.pl/hades/do/Login"
                                   :login-selector {:id :inputContent}
                                   :submit-login-selector [{:id :buttonTr} {:tag :a}]
                                   :before-fill-password (fn []
                                                           (wait-visible *driver* {:tag :frameset})
                                                           (switch-frame *driver* {:tag :frame :name :main}))
                                   :idx-from-input idx-from-input-id}))
