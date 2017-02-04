(ns smidje.cljs.macro-test
  (:require [smidje.core :refer-macros [fact tabular]])
  (:require-macros [cljs.test :refer [deftest is]]))

(enable-console-print!)

(defn bar []
  1)

(defn thing [var]
  1)

(defn foo []
  (+ (bar) (thing 1)))

(fact "multi-provided"
      (foo) => 2
      (provided
        (bar) => 0
        (thing 1) => 2))

(fact "name"
  (+ 1 1) => 2
  (+ 1 3) =not=> 2)

(tabular "tabularname"
         (fact "factname"
               (+ ?a ?b) => ?c)
         ?a ?b ?c
         1  2  3
         3  4  7)

(fact "truthy and falsey"
  true => truthy
  true => TRUTHY
  false => falsey
  false => FALSEY
  nil => falsey
  1 => truthy
  "text" => truthy)

(fact "expects exception"
  (throw (js/Error. "oh no!")) => (throws js/Error))

(fact "even is even"
      2 => even?
      3 =not=> even?
      (+ 3 2) => #(= 5 %))

(fact
  "provided works with truth checks"
  (bar) => truthy
  (bar) => falsey (provided (bar) => nil))

(fact
  "meta constant"
  (#(identity %) --test--) => --test--)

(fact
  "meta constant function"
  (#(--func-- %) --test--) => 1
  (provided
    (--func-- --test--) => 1))