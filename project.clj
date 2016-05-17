(defproject taxi-time-machine "0.1.0-SNAPSHOT"
  :description "See README"
  :url "https://github.com/trinityXmontoya/taxi-time-machine"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring "1.4.0"]
                 [http-kit "2.1.18"]
                 [environ "1.0.3"]
                 [com.taoensso/timbre "4.3.1"]
                 [yesql "0.5.2"]
                 [org.postgresql/postgresql "9.4-1206-jdbc4"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [com.zaxxer/HikariCP "2.4.3"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.0.3"]]
  :ring {:handler taxi-time-machine.core/handler}
  :main ^:skip-aot taxi-time-machine.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
