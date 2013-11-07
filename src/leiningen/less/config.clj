(ns leiningen.less.config
  (:require [leiningen.less.files :as files])
  (:import [java.nio.file Path]))

(defn- normalise [project raw-config]
  (let [^Path root (files/as-path (:root project))
        source-paths (get raw-config :source-paths (get project :resource-paths []))
        target-path (get raw-config :target-path (get project :target-path nil))
        source-paths (map (comp #(.resolve root ^Path %) files/as-path) source-paths)
        ^Path target-path (.resolve root (files/as-path target-path))]
    {:source-paths
      (vec
        (for [^Path source-path source-paths]
          (if-not (and (files/directory? source-path) (files/exists? source-path))
            (throw (IllegalArgumentException. (format "Invalid source path: %s" source-path)))
            (.toAbsolutePath source-path))))
     :target-path
      (.toAbsolutePath target-path)
     }))

(defn config [project]
  (when (nil? (:less project))
    (println "WARNING: no :less entry found in project definition."))
  (let [raw-config (:less project)]
    (normalise project raw-config)))
