(defproject restipe "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
    [cheshire "5.5.0"]
    [clj-http "2.0.0"]
    [com.cognitect/transit-clj "0.8.281"]
    [com.datomic/datomic-free "0.9.5206" :exclusions [joda-time]]
    [io.aviso/rook "0.1.36"]
    [org.clojure/clojure "1.7.0"]
    [prismatic/schema "0.4.3"]
    [ring "1.4.0"]
  ]
  :main ^:skip-aot restipe.server
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all} :dev {:dependencies [[speclj "3.3.1"]]}}
  :plugins [[speclj "3.3.1"]]
  :test-paths ["spec"])
