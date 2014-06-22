(defproject lein-less "1.7.2"
  :description "Less CSS compiler plugin for leiningen"
  :url "http://github.com/montoux/lein-less"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :java-source-paths ["java"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]

  :eval-in :leiningen
  :min-lein-version "2.3.0"

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.5.1" :optional true]
                                  [leiningen-core "2.3.4" :optional true]]}}
  )
