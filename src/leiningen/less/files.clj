(ns leiningen.less.files
  (:require [clojure.java.io :as jio])
  (:import (java.nio.file FileSystems FileSystem Files FileVisitor Path Paths FileVisitResult
                          WatchService WatchEvent WatchEvent$Kind WatchEvent$Modifier WatchKey
                          StandardWatchEventKinds)
           (java.nio.file.attribute BasicFileAttributes)))

;; Pre-computed arguments for nio interop function calls

(def follow-links (make-array java.nio.file.LinkOption 0))
(def utf-8 java.nio.charset.StandardCharsets/UTF_8)
(def default-open-options (make-array java.nio.file.OpenOption 0))
(def empty-string-array (make-array String 0))
(def continue java.nio.file.FileVisitResult/CONTINUE)
(def skip-tree java.nio.file.FileVisitResult/SKIP_SUBTREE)
(def default-attributes (make-array java.nio.file.attribute.FileAttribute 0))
(def watch-opts-cdm
  (into-array WatchEvent$Kind [StandardWatchEventKinds/ENTRY_CREATE
                               StandardWatchEventKinds/ENTRY_DELETE
                               StandardWatchEventKinds/ENTRY_MODIFY]))


(defprotocol PathCoercions
  "Coerce between various 'resource-namish' things. Intended for internal use."
  (^{:tag Path} as-path [x] "Coerce argument to a path."))


(extend-protocol PathCoercions
  nil
  (as-path [_] nil)

  String
  (as-path [s] (Paths/get ^String s empty-string-array))

  java.io.File
  (as-path [f] (.toPath ^java.io.File f))

  java.net.URL
  (as-path [u] (as-path (jio/file u)))

  java.net.URI
  (as-path [u] (Paths/get ^java.net.URI u))

  Path
  (as-path [p] p))

(defn fstr
  "Returns a string representing the file, relative to the project root."
  [project path]
  (.toString (.relativize (as-path (:root project)) (as-path path))))

(defn directory?
  "Returns true iff the pathish argument specifies a file system entity that is a directory."
  [path]
  (and (some-> path as-path (Files/isDirectory follow-links)) path))


(defn exists?
  "Returns true iff the pathish argument specifies a file system entity that exists."
  [path]
  (and (some-> path as-path (Files/exists follow-links)) path))

(defn less?
  "A predicate that tests whether the specified path is a less source file."
  [path]
  (let [fname (.toString (.getFileName (as-path path)))]
    (boolean
      (and fname
           (re-find #"[.]less$" fname)))))

(defn private?
  "A predicate that tests whether the specified path is 'private', that is, begins with '_'."
  [path]
  (let [fname (.toString (.getFileName (as-path path)))]
    (boolean
      (and fname (re-find #"^_" fname)))))

(defn descendents
  "Returns a list of descendents of the provided pathish root, possibly filtering with a predicate.
  If the predicate doesn't match a particular resource, none of its descendents will be included.
  Descendents includes the root."
  ([root] (descendents (constantly true) root))
  ([predicate root]
   (when (exists? root)
     (let [paths (atom [])]
       (Files/walkFileTree
         (as-path root)
         (proxy [java.nio.file.SimpleFileVisitor] []
           (visitFile [file attrs]
             (if (predicate file)
               (do (swap! paths conj file) continue)
               continue))
           (preVisitDirectory [dir attrs]
             (if (predicate dir)
               (do (swap! paths conj dir) continue)
               skip-tree))))
       @paths))))

(defn compilation-units [src-paths target-path]
  (let [src-paths (map as-path src-paths)
        ^Path target-path (as-path target-path)]
    (->>
      (for [^Path src-path src-paths
            ^Path src (remove private? (filter less? (descendents src-path)))]
        (let [dst ^Path (.resolve target-path (.relativize src-path src))
              [_ fname ext] (re-matches #"^(.+)[.]([^.]+)$" (.toString dst))
              dst (.resolve (.getParent dst) (format "%s.%s" fname "css"))]
          (when fname {:src src :dst dst})))
      (remove empty?)
      )))

(defn watch-resources [project paths callback]
  (let [^WatchService watcher (.newWatchService (FileSystems/getDefault))]
    (println "Found {less} source paths: ")
    (doseq [path paths
            child (descendents directory? path)]
      (println (format "  %s" (fstr project child)))
      (.register ^Path child watcher watch-opts-cdm))
    (println "Watching for changes...")
    (callback)
    (try
      (loop []
        (let [key (.take watcher)]
          (callback)
          (.pollEvents key)
          (.reset key)
          (recur)))
      (catch InterruptedException ex
        nil))))
