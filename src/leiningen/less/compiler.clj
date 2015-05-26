(ns leiningen.less.compiler
  (:refer-clojure :exclude [compile])
  (:require (leiningen.less [nio :as nio]
                            [engine :as engine])
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.nio.file Path]
           (java.io IOException)
           (javax.script ScriptEngineManager ScriptEngine ScriptContext)
           (leiningen.less LessError)))


(def version "1.7.5")
(def less-js (format "leiningen/less/less-rhino-%s.js" version))
(def lessc-js (format "leiningen/less/lessc.js"))


(defn initialise
  "Load less compiler resources required to compile less files to css. Must be called before invoking compile."
  []
  (engine/eval! (io/resource less-js) less-js)
  (engine/eval! (io/resource lessc-js) lessc-js))

(defn escape-string
  [s]
  (-> s str (string/replace #"\\" "\\\\\\\\")))

(defn compile-resource
  "Compile a single less resource."
  [src dst]
  (nio/create-directories (nio/parent dst))
  (engine/eval! (format "lessc.compile('%s', '%s');"
                        (escape-string (nio/absolute src))
                        (escape-string (nio/absolute dst)))))


(defn compile-project
  "Take a normalised project configuration and a sequence of src/dst pairs, compiles each pair."
  [project units on-error]
  (doseq [{:keys [^Path src ^Path dst]} units]
    (println (format "%s => %s" (nio/fstr project src) (nio/fstr project dst)))
    (try
      (compile-resource src dst)
      (catch LessError ex
             (on-error ex)))))
