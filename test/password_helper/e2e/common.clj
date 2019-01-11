(ns password-helper.e2e.common
  (:require [clojure.test :refer :all]
            [etaoin.api :refer :all]))

(def ^:dynamic *driver*)

(def project-dir (System/getProperty "user.dir"))

(def password "1234567890abcdefghijklmnopqrstuvwxyz")

(defn fixture-driver
  "Executes a test running a driver. Binds a driver
   with the global *driver* variable."
  [f]
  (with-chrome {:args [(str "--load-extension=" project-dir "/target/unpacked")]
                :port (+ 19999 (rand-int 1000))}
               driver
               (binding [*driver* driver]
                 (f))))

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
  "Returns the inputs idx based on the label for the input (uses aria)"
  [input]
  (let [id (get-element-attr-el *driver* input :aria-labelledby)
        label (query *driver* {:id id})
        text (get-element-text-el *driver* label)]
    (dec (Integer/parseInt text))))

(defn idx-from-sibling-label
  [input]
  (-> (js-execute *driver* "return arguments[0].parentElement.querySelector(\"label\").innerText;" (el->ref input))
      Integer/parseInt
      dec))

(defn idx-from-parent-text
  [input]
  (->> (js-execute *driver* "return arguments[0].parentElement.innerText;" (el->ref input))
       (re-find #"\d+")
       Integer/parseInt
       dec))

(defn find-password-inputs []
  (->> (query-all *driver* {:tag :input :type :password :maxlength 1})
       (remove #(get-element-attr-el *driver* % :readonly))
       (remove #(get-element-attr-el *driver* % :disabled))))

(defn verify-password-input-values [idx-from-input]
  (let [password-inputs (find-password-inputs)]
    (for [input password-inputs]
      (let [idx (idx-from-input input)
            char (str (nth password idx))]
        (is (= char (get-element-value-el *driver* input)) (str "idx=" idx " char=" char " input=" input))))))

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
  (wait *driver* 1)                                         ;; wait for the animation to finish, JS to steal focus and so on
  (fill-human *driver* {:id :password-helper-input} password)
  (let [test-results (verify-password-input-values idx-from-input)]
    (doall test-results)
    (after-fill-password)
    (if (some false? test-results)
      (postmortem-handler *driver* {:dir (str project-dir "/postmortems")}))))

(defn verify-picked-chars [picked-indexes]
  (let [password-inputs (find-password-inputs)]
    (for [n (range (count picked-indexes))]
      (let [idx (nth picked-indexes n)
            char (str (nth password idx))
            input (nth password-inputs n)]
        (is (= char (get-element-value-el *driver* input)) (str "idx=" idx " char=" char " input=" input))))))

(defn verify-typing-password-with-pick-chars [{:keys [login-url
                                                      login-selector
                                                      valid-login
                                                      before-submit-login
                                                      submit-login-selector
                                                      before-fill-password
                                                      picked-chars]
                                               :or {valid-login "123123123"
                                                    picked-chars [0 3 7]
                                                    before-submit-login #()
                                                    before-fill-password #()}}]
  (go *driver* login-url)
  (wait-visible *driver* login-selector)
  (fill *driver* login-selector valid-login)
  (before-submit-login)
  (click *driver* submit-login-selector)
  (before-fill-password)
  (wait-visible *driver* {:id :password-helper-input})
  (wait *driver* 1)                                         ;; wait for the animation to finish, JS to steal focus and so on
  (click *driver* {:css ".password-helper-open-pick-chars"})
  (fill-human *driver* {:id :password-helper-input} password)
  (doseq [idx picked-chars]
    (click *driver* {:id (str "pick-char-" idx)}))
  (let [test-results (verify-picked-chars picked-chars)]
    (doall test-results)
    (if (some false? test-results)
      (postmortem-handler *driver* {:dir (str project-dir "/postmortems")}))))
