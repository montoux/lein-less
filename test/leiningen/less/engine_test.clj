(ns leiningen.less.engine-test
  (:require [clojure.test :refer :all]
            [leiningen.less.engine :refer :all])
  (:import (leiningen.less LessError)))


(deftest test-default-engine
  (with-engine (create-engine)
    (is (= "a1" (eval! "'a'+1"))))
  (is (thrown? IllegalStateException (eval! "+1"))))


(deftest ^:rhino test-rhino-engine
  (with-engine "rhino"
    (is (= 1.0 (eval! "+1")))))


(deftest ^:rhino test-rhino-errors
  (with-engine "rhino"
    (is (thrown-with-msg? LessError #"^Error: Oops" (eval! "throw Error('Oops')")))
    (is (thrown-with-msg? LessError #"^Oops$"
                          (eval! "importClass(Packages.clojure.lang.RT);
                            var error = Packages.clojure.lang.RT['var']('leiningen.less.engine', 'error!')
                            error['invoke'](null, 'Oops');")))
    (is (thrown? NullPointerException
                 (eval! "importClass(Packages.clojure.lang.RT);
                         var rt_var = Packages.clojure.lang.RT['var'];
                         var npe = rt_var('clojure.core', 'read-string')['invoke']('(throw (NullPointerException.))');
                         rt_var('clojure.core', 'eval')['invoke'](npe);")))
    ))


(deftest ^:nashorn test-nashorn-engine
  (with-engine "nashorn"
    (is (= (eval! "+1")))
    (eval! "var x = 1")
    (is (= 1 (eval! "x")))
    (is (= 2 (eval! "Packages.clojure.lang.RT.var('clojure.core','inc').invoke(1)")))
    ))

(deftest ^:nashorn test-nashorn-errors
  (with-engine "nashorn"
    (is (thrown-with-msg? LessError #"^Error: Oops" (eval! "throw Error('Oops')")))
    (is (thrown-with-msg? LessError #"^Oops$"
                          (eval! "var error = Java.type('clojure.lang.RT')['var']('leiningen.less.engine', 'error!');
                            error['invoke'](null, 'Oops');")))
    (is (thrown? NullPointerException
                 (eval! "var rt_var = Java.type('clojure.lang.RT')['var'];
                         var npe = rt_var('clojure.core', 'read-string')['invoke']('(throw (NullPointerException.))');
                         rt_var('clojure.core', 'eval')['invoke'](npe);")))
    ))


(defmacro tests []
  `(vals (ns-interns ~*ns*)))


(defn test-ns-hook
  "Filter tests based on the type of JVM used to run the tests."
  []
  (let [jvm (System/getProperty "java.version")
        [_ M m] (clojure.string/split jvm #"\.")
        rhino? (#{"5" "6" "7"} M)
        nashorn? (#{"8" "9"} M)
        tests (filter (comp :test meta) (tests))
        tests (concat
                (remove (comp :rhino meta) (remove (comp :nashorn meta) tests))
                (when rhino? (filter (comp :rhino meta) tests))
                (when nashorn? (filter (comp :nashorn meta) tests)))]
    (doseq [test tests]
      (test))))
