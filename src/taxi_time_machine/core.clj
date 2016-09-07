(ns taxi-time-machine.core
  (:require [org.httpkit.server :refer [run-server]]
            [org.httpkit.client :as http]
            [environ.core :refer [env]]
            [taoensso.timbre :as timbre]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clj-kafka.producer :as kf-producer]
            [clj-kafka.admin :as kf-admin]
            [clojure.java.shell :as shell])
  ; (:import [org.geotools.data DataStoreFinder])
  (:gen-class))

(timbre/refer-timbre)

; mvn install:install-file -Dfile=jaad-0.8.3.jar -DartifactId=jaad -Dversion=0.8.3 -DgroupId=jaad -Dpackaging=jar -DlocalRepositoryPath=maven_repository
;

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp "resources/public/index.html")})

  ;  (println DataStoreFinder)

; ; OSRM
(defn extract-geometry
  [res]
  (get-in @res ["routes" 0 "geometry"]))
;
(defn get-route
  [row]
  (let [base "http://127.0.0.1:5000/route/v1/driving/"
        url (str base (row :pickup-lng) "," (row :pickup-lat) ";"
                      (row :dropoff-lng) "," (row :dropoff-lat)
                      "?geometries=geojson")]
    (http/get url nil
      (fn [{:keys [status headers body error]}]
        (if error
          (error "processing response" error)
          (json/decode body))))))

(defn calc-path
  [vals]
  (extract-geometry (get-route vals)))

(defn row->hash
  [row]
  (let [fields [:vendor-id :pickup-datetime :dropoff-datetime :passenger-count
                :trip-dist :pickup-lng :pickup-lat :rate-code-id :store-and-fwd-flag
                :dropoff-lng :dropoff-lat :payment-type :fare-amt :extra :mta-tax
                :tip-amt :tolls-amt :total-amt]]
      (zipmap fields row)))

;
; ; KAFKA
;
(def producer-conf (kf-producer/producer {"metadata.broker.list" "localhost:9092"
                  "serializer.class" "kafka.serializer.DefaultEncoder"
                  "partitioner.class" "kafka.producer.DefaultPartitioner"}))
;

(defn create-topic
  [name]
  (let [zk (kf-admin/zk-client "127.0.0.1:2181")]
  (kf-admin/create-topic zk name)))

(defn send-to-kafka
  [msg]
  (kf-producer/send-message producer-conf (kf-producer/message "butt" (.getBytes (json/encode msg)))))
;
; (defn run-osrm-server
;   []
;   (info "Running osrm server")
;   (println "me" (:out (shell/sh "osrm-routed" "/Users/Alfred/Downloads/us-northeast-latest.osrm"))))
;
; (defn start-zookeeper
;   []
;   (println "zookeeper" (:out (shell/sh "start-zk"))))
;
; (defn start-kafka
;   []
;   (println "kafka" (:out (shell/sh "start-kafka"))))

(defn -main
  []
  (println "im alive")
  (let [row ["1","2015-01-01 00:20:41","2015-01-01 00:27:07","1","1.20","-73.981498718261719","40.771186828613281","1","N","-73.972816467285156","40.782432556152344","2","7","0.5","0.5","0","0","8.3"]
      row-as-hash (row->hash row)
      path (calc-path row-as-hash)
      res (merge row-as-hash {:path path})]
      (send-to-kafka res)))

; (defn -main
;   [& args]
;   (let [port (Integer/parseInt (or (env :port) "8080"))]
;     (info "Server running on port" port)
;     (run-server handler {:port port})))
