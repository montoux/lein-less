(ns leiningen.less.config
  (:require [leiningen.less.nio :as nio])
  (:import [java.nio.file Path]))

(defn- normalise [project raw-config]
  (let [^Path root (nio/as-path (:root project))]
    (assert (nio/exists? root))
    {:source-paths (->> (get raw-config :source-paths (get project :resource-paths))
                        (map (partial nio/resolve root))
                        (map nio/absolute)
                        (filter nio/exists?) vec)
     :target-path (->> (get raw-config :target-path (get project :target-path nil))
                       (nio/resolve root)
                       (nio/absolute))}))

(defn config [project]
  (when (nil? (:less project))
    (println "WARNING: no :less entry found in project definition."))
  (let [raw-config (:less project)]
    (normalise project raw-config)))
