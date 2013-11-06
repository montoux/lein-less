(defproject montoux/lein-less "0.1.0-SNAPSHOT"
  :description "{less} css builder plugin for leiningen"
  :url "http://github.com/montoux/lein-less"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :eval-in-leiningen true

  :repositories [["releases" {:url "https://nexus.montoux.com/content/repositories/releases/"
                              :username [:gpg :env/nexus_username ]
                              :password [:gpg :env/nexus_password ]}]
                 ["snapshots" {:url "https://nexus.montoux.com/content/repositories/snapshots/"
                               :username [:gpg :env/nexus_username ]
                               :password [:gpg :env/nexus_password ]}]]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.lesscss/lesscss "1.3.3"]]

  :eval-in-leiningen true
  :min-lein-version "2.0.0"
  )
