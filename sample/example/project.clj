(let [[_ name version] (take 3 (clojure.string/split (slurp "../../project.clj") #"\s"))
      name (symbol name)
      version (second (re-matches #"^\"([-.\w]+)\"\s*$" version))]
  (defproject
    example "0.1.0-SNAPSHOT"
    :description "FIXME: write description"
    :url "http://example.com/FIXME"
    :license {:name "Eclipse Public License"
              :url  "http://www.eclipse.org/legal/epl-v10.html"}
    :dependencies [[org.clojure/clojure "1.6.0"]]
    :plugins [[~(symbol name) ~version]]
    :hooks [leiningen.less]
    ))
