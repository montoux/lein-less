(ns leiningen.less.compiler
  (:refer-clojure :exclude [compile])
  (:require [leiningen.less.files :as files])
  (:import [org.lesscss LessCompiler LessException]
           [java.nio.file Path]
           (java.io IOException)))

(def ^:private compiler (LessCompiler.))

(defn compile [project units rethrow-errors?]
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
