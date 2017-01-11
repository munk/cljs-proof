(ns smidje.core-test
  (:require
    #?(:cljs [smidje.cljs-generator.test-builder])
    #?(:clj [smidje.parser.parser :as parser])))

(defmacro fact [& args]
  (let [test-configuration# (parser/parse-fact &form)
        test-name           (gensym)]
    `(cljs.core/let [test-configuration# ~test-configuration#]
       (cljs.test/deftest ~test-name
         (cljs.test/is (cljs.core/= 1 0))
         smidje.cljs-generator.test-builder/generate-tests test-configuration#))))

(defmacro tabular [& _]
  (parser/tabular* &form))

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
