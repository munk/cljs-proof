(ns smidje.cljs-generator.test-builder
  (:require [smidje.parser.arrows :refer [arrow-set]]
            [smidje.symbols :refer [anything]]
            [smidje.parser.checkers :refer [truthy falsey TRUTHY FALSEY truth-set]]
            [smidje.cljs-generator.cljs-syntax-converter :refer [clj->cljs]]
            [clojure.test :refer [deftest is]]))

(defn do-arrow [arrow]
  (cond
    (= arrow '=>) '=
    (= arrow '=not=>) 'not=
    :else (throw (Exception. (format "Unknown arrow given: %s | Valid arrows: %s"
                                     arrow
                                     arrow-set)))))

(defn do-truth-test [form]
  (cond
    (= form 'truthy) true
    (= form 'TRUTHY) true
    (= form 'falsey) false
    (= form 'FALSEY) false
    :else (throw (Exception. (format "Unknown truth testing expression: %s | Valid expressions: %s"
                                     form truth-set)))))

(defn generate-mock-binding [mocks-atom]
  (fn [mock-data-map]
    (let [[function-key {function :function}] mock-data-map
          mock-function-template `(smidje.core/generate-mock-function ~function-key ~mocks-atom)]
      [function mock-function-template])))

(defn generate-mock-bindings [provided mocks-atom]
  (into [] (reduce concat (map (generate-mock-binding mocks-atom) provided))))

(defn generate-mock-map [provided]
  (reduce
    (fn [current-map addition]
      (let [{mock-config :return
             function    :mock-function} addition
            function-key (str function)]
        (merge current-map {function-key {:mock-config mock-config
                                          :function    function}})))
    {}
    provided))

(defn generate-single-assert [assertion]
  (let [{arrow           :arrow
         test-function   :call-form
         expected-result :expected-result} assertion]
    ;we bind expected-result and test-function to vars so that if they are nil we don't get a compilation error
    `(let [expected-result-var# ~expected-result
           test-function-var# ~test-function]
       (cond
        (fn? expected-result-var#) (is (~(do-arrow arrow) (expected-result-var# test-function-var#) true))
        (= expected-result-var# anything) (do test-function-var# (is true))
        :else (is (~(do-arrow arrow) test-function-var# expected-result-var#))))))

(defn generate-truth-test [truth-test-definition]
  (let [truth-type# (:truth-testing truth-test-definition)
        test-function# (:call-form truth-test-definition)]
    `(is (= (boolean ~test-function#) ~(do-truth-test truth-type#)))))

(defn generate-expected-exception [exception-definition]
  (let [expected-exception (:throws-exception exception-definition)
        call-form (:call-form exception-definition)]
    `(is (~'thrown? ~(symbol expected-exception) ~call-form))))

(defn generate-assertion
  [assertion]
  (cond
    (:truth-testing assertion) (generate-truth-test assertion)
    (:throws-exception assertion) (generate-expected-exception assertion)
    :else (generate-single-assert assertion)))

(defn list-contains? [list object]
  (if (empty? list)
    false
    (some #(= object %) list)))

(defn extract-metaconstant-mocks [metaconstants mock-map]
  (let [metaconstant-list (keys metaconstants)]
    (into
      {}
      (filter
        (fn [[keystring :as mock]]
          (when (list-contains? metaconstant-list (symbol keystring))
            mock))
        mock-map))))

(defn generate-wrapped-assertion [metaconstants assertion]
  (let [{provided# :provided} assertion
        complete-mock-map (generate-mock-map provided#)
        mocks-atom (gensym "mocks-atom")
        unbound-mocks (extract-metaconstant-mocks metaconstants complete-mock-map)
        bound-mocks (apply dissoc complete-mock-map (keys unbound-mocks))]
    `(let ~(into [] (concat [mocks-atom `(atom ~complete-mock-map)]
                            (generate-mock-bindings unbound-mocks mocks-atom)))
       (with-redefs ~(generate-mock-bindings bound-mocks mocks-atom)
         ~(generate-assertion assertion)
          (smidje.core/validate-mocks ~mocks-atom)))))

(defn generate-metaconstant-bindings [metaconstants]
   (->> (map
          (fn [metaconstant]
            [metaconstant
             (name metaconstant)])
          (keys metaconstants))
        (reduce concat)
        (into [])))

(defn generate-test [{assertions# :assertions
                      name# :name
                      metaconstants# :metaconstants}]
  (cond
    (empty? assertions#) `(print "warning:" ~name# "does not have any assertions and will be ignored")
    :else `(deftest ~(symbol name#)
             (let ~(generate-metaconstant-bindings metaconstants#)
               ~@(map
                   (partial generate-wrapped-assertion metaconstants#)
                   assertions#)))))

(defn generate-tests [test-runtime]
  (let [tests# (:tests test-runtime)]
    (clj->cljs `(do ~@(map generate-test tests#)))))
