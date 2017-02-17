(ns smidje.core
  (:require #?(:clj [smidje.cljs-generator.test-builder :as cljs-builder])
            #?(:clj [smidje.parser.parser :as parser])
            #?(:cljs [cljs.test :refer-macros [deftest is]])
            [smidje.cljs-generator.mocks :as mocks]
            [smidje.symbols :as symbols]))

;Namespaced symbols to differentiate between smidje syntax and user variables
(def anything symbols/anything)

(defmacro fact [& args]
  (-> (parser/parse-fact &form)
      cljs-builder/generate-tests))

(defmacro tabular [& _]
  (parser/tabular* &form))

;------functions exposed for use by generated code-------

(defn generate-mock-function [function-key mocks-atom]
  (mocks/generate-mock-function function-key mocks-atom))

(defn validate-mocks [mocks-atom]
  (mocks/validate-mocks mocks-atom))

(comment
  (macroexpand
    '(fact "what a fact"
           (+ 1 1) => 2
           (+ 2 2) =not=> 3
           "hi" => truthy
           true => TRUTHY
           false => FALSEY
           (/ 2 0) => (throws ArithmeticException)
           (/ 4 0) => (throws ArithmeticException "Divide by zero")))

  (macroexpand
   '(tabular "test name"
             (fact "fact name"
                   (+ ?a ?b) => ?c)
             ?a ?b ?c
             1  2  3
             3  4  7))
  )
