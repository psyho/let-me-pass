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
  (with-chrome {:args [(str "--load-extension=" project-dir "/target/unpacked")]
                :port (+ 19999 (rand-int 1000))}
               driver
               (binding [*driver* driver]
                 (f))))

(use-fixtures
  :each ;; start and stop driver for each test
  fixture-driver)

(defn idx-from-input-based-on-attr [input attr]
  (let [id (get-element-attr-el *driver* input attr)
        number (re-find #"\d+" id)]
    (dec (Integer/parseInt number))))

(defn idx-from-input-id
  "Returns the index corresponding to the password input, based on ID attribute"
  [input]
  (idx-from-input-based-on-attr input :id))

(defn idx-from-input-name
  "Returns the index corresponding to the password input, based on name attribute"
  [input]
  (idx-from-input-based-on-attr input :name))

(defn verify-typing-input-via-helper [{:keys [login-url
                                              login-selector
                                              valid-login
                                              before-submit-login
                                              submit-login-selector
                                              before-fill-password
                                              idx-from-input
                                              after-fill-password]
                                       :or {valid-login "123123123"
                                            before-submit-login #()
                                            before-fill-password #()
                                            after-fill-password #()}}]
  (go *driver* login-url)
  (wait-visible *driver* login-selector)
  (fill *driver* login-selector valid-login)
  (before-submit-login)
  (click *driver* submit-login-selector)
  (before-fill-password)
  (wait-visible *driver* {:id :password-helper-input})
  (fill *driver* {:id :password-helper-input} password)
  (let [password-inputs
        (->> (query-all *driver* {:tag :input :type :password :maxlength 1})
             (remove #(get-element-attr-el *driver* % :readonly))
             (remove #(get-element-attr-el *driver* % :disabled)))]
    (doseq [input password-inputs]
      (let [idx (idx-from-input input)
            char (str (nth password idx))]
        (is (= char (get-element-value-el *driver* input))))))
  (after-fill-password))

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

(deftest alior-new
  (verify-typing-input-via-helper {:login-url "https://system.aliorbank.pl/sign-in"
                                   :login-selector {:id :login}
                                   :valid-login "45573286"
                                   :submit-login-selector {:tag :button :title "Next" :type :submit}
                                   :idx-from-input idx-from-input-name
                                   :after-fill-password (fn []
                                                          (let [password-submit-btn (query *driver* {:id :password-submit})
                                                                disabled (get-element-attr-el *driver* password-submit-btn :disabled)]
                                                            (is (nil? disabled))))}))
