(ns password-helper.e2e-test
  (:require [clojure.test :refer :all]
            [etaoin.api :refer :all]))

(def ^:dynamic *driver*)

(def project-dir (System/getProperty "user.dir"))

(def password "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")

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

(defn idx-from-input-based-on-attr
  ([input attr]
    (idx-from-input-based-on-attr input attr dec))

  ([input attr adjust-fn]
   (let [id (get-element-attr-el *driver* input attr)
         number (re-find #"\d+" id)]
     (adjust-fn (Integer/parseInt number)))))

(defn idx-from-input-id
  "Returns the index corresponding to the password input, based on ID attribute"
  [input]
  (idx-from-input-based-on-attr input :id))

(defn idx-from-input-name
  "Returns the index corresponding to the password input, based on name attribute"
  [input]
  (idx-from-input-based-on-attr input :name))

(defn idx-from-position-among-other-inputs
  "Returns the inputs position in a table row as the index"
  [input]
  (let [inputs (query-all *driver* {:tag :input :type :password :maxlength 1})]
    (->> inputs
         (map-indexed vector)
         (filter #(= (second %) input))
         first
         first)))

(defn idx-from-aria-label
  "Returns the inputs idx based on the label for the input"
  [input]
  (let [id (get-element-attr-el *driver* input :aria-labelledby)
        label (query *driver* {:id id})
        text (get-element-text-el *driver* label)]
    (dec (Integer/parseInt text))))

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
        (is (= char (get-element-value-el *driver* input)) (str "idx=" idx " char=" char " input=" input)))))
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
(deftest pekao
  (verify-typing-input-via-helper {:login-url "http://demo.pekao24.pl/"
                                   :login-selector {:id :parUsername}
                                   :submit-login-selector {:id :butLogin}
                                   :idx-from-input #(idx-from-input-based-on-attr % :id identity)}))

(deftest bgz-pnb-paribas
  (verify-typing-input-via-helper {:login-url "https://planet.bgzbnpparibas.pl/hades/ver/pl/demo_smart_planet/index2.html"
                                   :login-selector {:class :login-input}
                                   :submit-login-selector {:tag :input :type :submit :class :greenButton}
                                   :idx-from-input idx-from-position-among-other-inputs}))

(deftest bos-bank
  (verify-typing-input-via-helper {:login-url "https://bosbank24.pl/twojekonto"
                                   :login-selector {:id :login_id}
                                   :submit-login-selector {:tag :img :alt "dalej" :title "dalej"}
                                   :idx-from-input #(idx-from-input-based-on-attr % :id identity)}))

(deftest bz-wbk
  (verify-typing-input-via-helper {:login-url             "https://www.centrum24.pl/centrum24-web/login"
                                   :valid-login           "12312312"
                                   :login-selector        {:id :input_nik}
                                   :submit-login-selector {:tag :input :name "loginButton"}
                                   :idx-from-input        idx-from-aria-label}))

(deftest envelo-bank
  (verify-typing-input-via-helper {:login-url             "https://online.envelobank.pl/login/main"
                                   :login-selector        {:id :user-alias}
                                   :submit-login-selector {:tag :input :type :submit :value "DALEJ"}
                                   :idx-from-input        #(idx-from-input-based-on-attr % :data-password-field-number dec)}))

(deftest idea-bank
  (verify-typing-input-via-helper {:login-url             "https://secure.ideabank.pl/"
                                   :valid-login           "111222"
                                   :login-selector        {:id :log}
                                   :submit-login-selector {:tag :button :type :submit :class "dalej1"}
                                   :idx-from-input        #(idx-from-input-based-on-attr % :id identity)}))

(deftest getin-bank
  (verify-typing-input-via-helper {:login-url             "https://secure.getinbank.pl"
                                   :valid-login           "111222"
                                   :login-selector        {:tag :input :name :login}
                                   :submit-login-selector {:tag :button :type :submit}
                                   :idx-from-input        #(idx-from-input-based-on-attr % :name identity)}))
