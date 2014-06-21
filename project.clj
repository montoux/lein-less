(defproject lein-less "1.7.2-SNAPSHOT"
  :description "Less CSS compiler plugin for leiningen"
  :url "http://github.com/montoux/lein-less"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0" :optional true]
                 [org.mozilla/rhino "1.7R4" :optional true]]

  :java-source-paths ["java"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]

  :eval-in :leiningen
  :min-lein-version "2.3.0"
  )
