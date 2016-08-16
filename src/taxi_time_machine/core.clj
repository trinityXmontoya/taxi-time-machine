(ns taxi-time-machine.core
  (:require [org.httpkit.server :refer [run-server]]
            [org.httpkit.client :as http]
            [environ.core :refer [env]]
            [taoensso.timbre :as timbre]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clj-kafka.producer :as producer]
            [clojure.java.shell :as shell])
  (:gen-class))

(timbre/refer-timbre)

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp "resources/public/index.html")})

; OSRM
(defn extract-geometry
  [res]
  (get-in @res ["routes" 0 "geometry"]))

(defn get-route
  [row]
  (let [base "http://127.0.0.1:5000/route/v1/driving/"
        url (str base (get row 5) "," (get row 6) ";" (get row 9) "," (get row 10) "?geometries=geojson")]
    (http/get url nil
      (fn [{:keys [status headers body error]}]
        (if error
          (error "processing response" error)
          (json/decode body))))))

(defn add-path
  [row]
  (let [geometry (extract-geometry (get-route row))]
    (println geometry)))

; KAFKA

(def producer-conf (producer/producer {"metadata.broker.list" "localhost:9999"
                  "serializer.class" "kafka.serializer.DefaultEncoder"
                  "partitioner.class" "kafka.producer.DefaultPartitioner"}))

(defn send-to-kafka
  [msg]
  (producer/send-message producer-conf (producer/message msg (.getBytes "this is my message"))))

(defn run-osrm-server
  []
  (info "Running osrm server")
  (println "me" (:out (shell/sh "osrm-routed" "/Users/Alfred/Downloads/us-northeast-latest.osrm"))))

(defn run-zookeeper
  []
  (println "zookeeper" (:out (shell/sh "/Users/Alfred/Downloads/kafka-0.8.2.2-src/bin/zookeeper-server-start.sh" "/Users/Alfred/Downloads/kafka-0.8.2.2-src/config/zookeeper.properties"))))

(defn -main
  []
  (println "im alive")
  ; (add-path ["1","2015-01-01 00:20:41","2015-01-01 00:27:07","1","1.20","-73.981498718261719","40.771186828613281","1","N","-73.972816467285156","40.782432556152344","2","7","0.5","0.5","0","0","8.3"])
  )

; (defn -main
;   [& args]
;   (let [port (Integer/parseInt (or (env :port) "8080"))]
;     (info "Server running on port" port)
;     (run-server handler {:port port})))
