(defproject taxi-time-machine "0.1.0-SNAPSHOT"
  :description "See README"
  :url "https://github.com/trinityXmontoya/taxi-time-machine"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring "1.4.0"]
                 [http-kit "2.1.18"]
                 [environ "1.0.3"]
                 [com.taoensso/timbre "4.3.1"]
                 [cheshire "5.6.3"]
                 [clj-kafka "0.3.4"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.0.3"]]
  :ring {:handler taxi-time-machine.core/handler}
  :main ^:skip-aot taxi-time-machine.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
