(ns leiningen.less
  (:require (leiningen compile help)
            (leiningen.core.main)
            (leiningen.less
              [compiler :as compiler]
              [config :refer [config]]
              [nio :as nio])
            [robert.hooke :as hooke]
            [leiningen.less.engine :as engine])
  (:import (leiningen.less LessError)))


(defn- report-error [^LessError error]
  (binding [*out* *err*]
    (println (.getMessage error))))

(defn- abort-on-error [^LessError error]
  (leiningen.core.main/abort (.getMessage error)))

(defn- run-compiler
  "Run the lesscss compiler."
  [project {:keys [source-paths target-path] :as config} watch?]
  (engine/with-engine "javascript"
    (compiler/initialise)
    (println "Compiling {less} css:")
    (let [units (nio/compilation-units source-paths target-path)
          on-error (if watch? report-error abort-on-error)
          compile (partial compiler/compile-project project units on-error)]
      (if watch?
        (nio/watch-resources project source-paths compile)
        (compile))
      (println "Done."))))

(defn- once
  "Compile less files once."
  [project config]
  (run-compiler project config false))

(defn- auto
  "Compile less files, then watch for changes and recompile until interrupted."
  [project config]
  (run-compiler project config true))

(defn less
  "Run the {less} css compiler plugin."
  {:help-arglists '([once auto])
   :subtasks      [#'once #'auto]}
  ([project]
   (println (leiningen.help/help-for "less"))
   (leiningen.core.main/abort))
  ([project subtask & args]
   (let [config (config project)]
     (case subtask
       "once" (apply once project config args)
       "auto" (apply auto project config args)
       )))
  )

(defn compile-hook [task & args]
  (apply task args)
  (run-compiler (first args) (config (first args)) false))

(defn activate
  "Set up hooks for less compilation."
  []
  (hooke/add-hook #'leiningen.compile/compile #'compile-hook))
