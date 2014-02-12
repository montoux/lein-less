(ns leiningen.less.compiler
  (:refer-clojure :exclude [compile])
  (:require (leiningen.less [files :as files]
                            [engine :as engine])
            [clojure.java.io :as io])
  (:import [java.nio.file Path]
           (java.io IOException)
           (javax.script ScriptEngineManager ScriptEngine ScriptContext)))


(def version "1.6.3")
(def utils (format "leiningen/less/utils.js"))
(def less-js (format "leiningen/less/less-rhino-%s.js" version))
(def lessc-js (format "leiningen/less/lessc-rhino-%s.js" version))

(defprotocol JSReader
  (^String readFile [^String filename ^String charset]))

(deftype JSIO []
  JSReader
  (readFile [filename charset]
    (slurp filename)))

(def engine
  (delay
    (doto engine/default-engine
      (engine/eval! (io/reader (io/resource less-js)) less-js)
      (engine/eval! (io/reader (io/resource utils)) utils)
      (.put "leiningen_less_io" (JSIO.))
      )))

(defn compile
  ([^Path src ^Path dst]
   (let [engine @engine]
     (engine/eval! engine (format "var arguments = ['%s', '%s'];" (files/absolute src) (files/absolute dst)))
     (engine/eval! engine (io/reader (io/resource lessc-js)) lessc-js)
     ))
  ([project units rethrow-errors?]
   (doseq [{:keys [^Path src ^Path dst]} units]
     (println (format "  %s => %s" (files/fstr project src) (files/fstr project dst)))
     (compile src dst)))
  )


#_ (defn compile [project units rethrow-errors?]
  (doseq [{:keys [^Path src ^Path dst]} units]
    (println (format "  %s => %s" (files/fstr project src) (files/fstr project dst)))
    (try
      (.compile ^LessCompiler compiler (.toFile src) (.toFile dst))
      (catch IOException ex
        (if rethrow-errors?
          (throw ex)
          (println (.getMessage ex))))
      (catch LessException ex
        (if rethrow-errors?
          (throw ex)
          (do (println (.getMessage ex))
              (println (format "(%s)" (.getMessage (.getCause ex))))))
        ))))
