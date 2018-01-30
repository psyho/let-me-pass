(ns password-helper.content-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [password-helper.content :refer [first-number at-index]]))

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

(deftest at-index-test
  (testing "blank"
    (is (= "" (at-index "" 10)))
    (is (= "" (at-index nil 10))))

  (testing "out of bounds"
    (is (= "" (at-index "foo" 10)))
    (is (= "" (at-index "foo" 3))))

  (testing "within bounds"
    (is (= "f" (at-index "foo" 0)))
    (is (= "c" (at-index "abc" 2))))

  (testing "last"
    (is (= "d" (at-index "abcd" -1)))
    (is (= "" (at-index "" -1))))

  (testing "second last"
    (is (= "c" (at-index "abcd" -2)))
    (is (= "" (at-index "d" -2))))

  (testing "negative"
    (is (= "b" (at-index "abcd" -3)))
    (is (= "a" (at-index "abcd" -4)))
    (is (= "" (at-index "abcd" -5)))))
