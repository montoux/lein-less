(ns leiningen.less
  (:refer-clojure :exclude [compile])
  (:require (leiningen compile help)
            (leiningen.core.main)
            (leiningen.less
              [compiler :refer [compile]]
              [config :refer [config]]
              [files :as files])
            [robert.hooke :as hooke]))

(defn- run-compiler
  "Run the lesscss compiler."
  [project {:keys [source-paths target-path] :as config} watch?]
  (println "Compiling {less} css:")
  (let [units (files/compilation-units source-paths target-path)]
    (if watch?
      (files/watch-resources project source-paths (partial compile project units false))
      (compile project units true))
    (println "Done.")))

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
