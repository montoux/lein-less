(let [[_ name version] (take 3 (clojure.string/split (slurp "../../project.clj") #"\s"))
      name (symbol name)
      version (second (re-matches #"^\"([-.\w]+)\"\s*$" version))]
  (defproject
    example2 "0.1.0-SNAPSHOT"
    :plugins [[~(symbol name) ~version]]
    :less {:source-paths ["less"]}
    ))
