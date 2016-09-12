(defproject taxi-time-machine "0.1.0-SNAPSHOT"
  :description "See README"
  :url "https://github.com/trinityXmontoya/taxi-time-machine"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring "1.4.0"]
                 [http-kit "2.1.18"]
                 [environ "1.0.3"]
                 [com.taoensso/timbre "4.3.1"]
                 [cheshire "5.6.3"]
                 [clojure-csv/clojure-csv "2.0.1"]
                 [org.clojure/data.csv "0.1.3"]
                 [clj-time "0.12.0"]
                 [kafka-clj "3.6.5"]
                 [org.locationtech.geomesa/geomesa-kafka-datastore "1.2.5"]
                ;  [org.apache.kafka/kafka_2.11 "0.8.2.1"]
                 ]
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.0.3"]
            [lein-localrepo "0.5.3"]]
  :ring {:handler taxi-time-machine.core/handler}
  :main ^:skip-aot taxi-time-machine.geomesa
  :target-path "target/%s"
  :repositories {"locationtech-releases" "https://repo.locationtech.org/content/groups/releases"
                 "geomesa-snapshots" "https://repo.locationtech.org/content/repositories/geomesa-snapshots"
                 "boundlessgeo" "http://repo.boundlessgeo.com/main"
                 "osgeo" "http://download.osgeo.org/webdav/geotools"
                 "conjars.org" "http://conjars.org/repo"}
  :profiles {:uberjar {:aot :all}})
