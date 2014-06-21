(ns leiningen.less.nio
  (:refer-clojure :exclude [resolve])
  (:require [clojure.java.io :as jio])
  (:import (java.nio.file FileSystems FileSystem Files FileVisitor Path Paths FileVisitResult
                          WatchService WatchEvent WatchEvent$Kind WatchEvent$Modifier WatchKey
                          StandardWatchEventKinds LinkOption OpenOption SimpleFileVisitor)
           (java.nio.file.attribute BasicFileAttributes FileAttribute)
           (java.io Reader File BufferedReader InputStreamReader)
           (java.nio.charset StandardCharsets)
           (java.net URL URI)))

;; Pre-computed arguments for nio interop function calls

(def follow-links (make-array LinkOption 0))
(def utf-8 StandardCharsets/UTF_8)
(def default-open-options (make-array OpenOption 0))
(def empty-string-array (make-array String 0))
(def continue FileVisitResult/CONTINUE)
(def skip-tree FileVisitResult/SKIP_SUBTREE)
(def default-attributes (make-array FileAttribute 0))
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

  File
  (as-path [^File f] (.toPath f))

  URL
  (as-path [^URL u] (Paths/get (.toURI u)))

  URI
  (as-path [^URI u] (Paths/get u))

  Path
  (as-path [p] p))

(extend-protocol jio/Coercions
  Path
  (as-file [^Path p] (.toFile p))
  (as-url [^Path p] (.toURL p)))

(extend Path
  jio/IOFactory
  (assoc jio/default-streams-impl
    :make-input-stream (fn [^Path p _] (Files/newInputStream p default-open-options))
    :make-output-stream (fn [^Path p opts] (Files/newOutputStream p default-open-options))))

(defn fstr
  "Returns a string representing the file, relative to the project root."
  [project path]
  (.toString (.relativize (as-path (:root project)) (as-path path))))

(defn resolve
  "Resolve the pathish argument relative the the first."
  ^Path [context to-resolve]
  (when-let [context ^Path (as-path context)]
    (if-let [rel ^Path (as-path to-resolve)]
      (.resolve context rel)
      (throw (IllegalArgumentException. (str "Cannot resolve " to-resolve))))))

(defn parent
  "Given a pathish argument, get its parent. This will fail if the path doesn't specify a parent."
  ^Path [path]
  (some-> path as-path .getParent))

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

(defn directory?
  "Returns true iff the pathish argument specifies a file system entity that is a directory."
  [path]
  (and (some-> path as-path (Files/isDirectory follow-links)) path))

(defn exists?
  "Returns true iff the pathish argument specifies a file system entity that exists."
  [path]
  (and (some-> path as-path (Files/exists follow-links)) path))

(defn absolute
  "Given a pathish argument, creates an absolute java.nio.file.Path."
  ^Path [pathish]
  (.toAbsolutePath (as-path pathish)))

(defn resource
  "Resolve a classpath resource to a java.nio.file.Path. Expects a string."
  ^Path [name]
  (as-path (jio/resource name)))

(defn create-directories
  "Creates the specified path as a directory using java.nio.path.Files/createDirectories."
  ^Path [dir]
  (Files/createDirectories (as-path dir) default-attributes))

(defn create-temp-directory
  "Creates a new temporary directory with the specified prefix name in the OS-specified temporary file-system location."
  ^Path [^String prefix]
  (Files/createTempDirectory prefix default-attributes))

(defn- remove-recursive [^Path to-remove]
  (let [removed (atom nil)]
    (Files/walkFileTree to-remove
                        (proxy [SimpleFileVisitor] []
                          (visitFile [file attrs] (Files/delete file) (swap! removed conj file) continue)
                          (visitFileFailed [file exc] (Files/delete file) (swap! removed conj file) continue)
                          (postVisitDirectory [dir exc] (Files/delete dir) (swap! removed conj dir) continue)))
    (set @removed)))

(defn delete-recursively
  "Recursively delete all files rooted at the specified path. Returns a set of the deleted paths."
  [root]
  (let [root (as-path root)]
    (when (exists? root)
      (remove-recursive (as-path root)))))

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
         (proxy [SimpleFileVisitor] []
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
