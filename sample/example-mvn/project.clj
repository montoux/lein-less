(defproject com.example/example-mvn "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :less {:source-paths ["src/main/resources"]
         :target-path  "target/generated"}
  :plugins [[montoux/lein-less "1.6.3-SNAPSHOT"]]
  :hooks [leiningen.less]
)
