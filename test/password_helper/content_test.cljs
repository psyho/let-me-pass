(ns password-helper.content-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [password-helper.content :refer [first-number]]))

(deftest first-number-test
  (testing "blank"
    (is (= nil (first-number "")))
    (is (= nil (first-number nil))))

  (testing "text contains digits"
    (is (= "123" (first-number "hello 123")))
    (is (= "123" (first-number "hello123")))
    (is (= "123" (first-number "field-123")))
    (is (= "123" (first-number "123 234"))))

  (testing "no digits"
    (is (= nil (first-number "hello")))
    (is (= nil (first-number "hello world"))))

  (testing "last"
    (is (= "0" (first-number "last")))
    (is (= "0" (first-number "Last")))
    (is (= "0" (first-number "the last character"))))

  (testing "second last"
    (is (= "-1" (first-number "second last")))
    (is (= "-1" (first-number "second to last")))
    (is (= "-1" (first-number "SECOND last")))))
