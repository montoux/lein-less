(ns leiningen.less.compiler-test
  (:import (leiningen.less LessError))
  (:require [clojure.test :refer :all]
            [leiningen.less.compiler :refer :all]
            [leiningen.less.engine :as engine]
            [leiningen.less.nio :as nio]))


(def ^:dynamic *tmp* nil)

(defn with-tmp-dir [test]
  (fn []
    (let [tmp (nio/create-temp-directory "less-compiler-test")]
      (try (binding [*tmp* tmp] (test))
           (finally (nio/delete-recursively tmp))))))

(defn with-engine [test]
  (fn run-test-with-engine []
    (engine/with-engine "javascript"
      (initialise)
      (test))))


(use-fixtures :each (comp (fn run [f] (f)) with-tmp-dir with-engine))


(deftest test-compile-simple
  (let [input (nio/resolve *tmp* "simple-test.less")
        output (nio/resolve *tmp* "simple-test.css")
        content "a { b { c: d; }}"
        _ (spit input content)]
    (testing "preconditions"
      (is (= content (slurp input)))
      (is (not (nio/exists? output))))

    (is (= 0 (int (compile-resource input output))))

    (when (is (nio/exists? output))
      (is (= "a b {\n  c: d;\n}\n" (slurp output))))
    ))


(deftest test-compile-error
  (let [input (nio/resolve *tmp* "error-test.less")
        output (nio/resolve *tmp* "error-test.css")
        content "a { b { c: d; }"                           ;; missing brace
        _ (spit input content)]
    (testing "preconditions"
      (is (= content (slurp input)))
      (is (not (nio/exists? output))))

    (is (thrown-with-msg? LessError #"^ParseError: missing closing `}`"
                          (compile-resource input output)))

    (is (not (nio/exists? output)))
    ))
