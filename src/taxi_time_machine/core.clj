(ns taxi-time-machine.core
  (:require [org.httpkit.server :refer [run-server]]
            [org.httpkit.client :as http]
            [environ.core :refer [env]]
            [taoensso.timbre :as timbre]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            ; [clj-kafka.new.producer :as kf-producer]
            ; [clj-kafka.admin :as kf-admin]
            ; [kafka-clj.client :as kafka]
            [taxi-time-machine.geomesa :as geomesa]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [clojure.java.shell :as shell])
  (:import (org.locationtech.geomesa.utils.text WKTUtils$))
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

(def custom-formatter (f/formatter "yyyy-MM-dd HH:mm:ss"))
(defn ->datetime [str] (f/parse custom-formatter str))
(defn ->java-date [^org.joda.time.DateTime datetime] (c/to-date datetime))


; ; OSRM
(defn calc-route
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

(defn build-point
  [coord]
  (.read WKTUtils$/MODULE$ (str "POINT(" (clojure.string/join " " coord) ")")))

(defn calc-trip-time
  [start end]
  (t/in-seconds (t/interval start end)))

(defn calc-stops
  [trip]
  (let [route (calc-route trip)
        coords (get-in @route ["routes" 0 "geometry" "coordinates"])
        total-trip-secs (calc-trip-time (trip :pickup-datetime) (trip :dropoff-datetime))
        secs-per-path (/ total-trip-secs (- (count coords) 1))
        start (trip :pickup-datetime)]
    (map-indexed (fn [i coord]
                    (let [point (build-point coord)
                          datetime (c/to-date (t/plus start (t/seconds (* secs-per-path i))))]
                      {:geom point :datetime datetime})) coords)))

(defn row->hash
  [row]
  (let [fields [:vendor-id :pickup-datetime :dropoff-datetime :passenger-count
                :trip-dist :pickup-lng :pickup-lat :rate-code-id :store-and-fwd-flag
                :dropoff-lng :dropoff-lat :payment-type :fare-amt :extra :mta-tax
                :tip-amt :tolls-amt :total-amt]
       res (zipmap fields row)]
       (assoc res :pickup-datetime (->datetime (res :pickup-datetime))
                   :dropoff-datetime (->datetime (res :dropoff-datetime)))))

;
; ; KAFKA
;
; (def producer-conf (kf-producer/producer {"bootstrap.servers" "localhost:9092"}
; (kf-producer/byte-array-serializer) (kf-producer/byte-array-serializer)))
; (def producer-conf (kafka/create-connector [{:host "localhost" :port 9092}] {:flush-on-write true}))
; ;
;
; (defn create-topic
;   [name]
;   (let [zk (kf-admin/zk-client "127.0.0.1:2181")]
;   (kf-admin/create-topic zk name)))
;
; (defn send-to-kafka
;   [msg]
;   (kafka/send-msg producer-conf "butt" (.getBytes (json/encode msg))))
;   @(kf-producer/send producer-conf (kf-producer/record "butt" (.getBytes (json/encode msg)))))
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

        trip (row->hash row)

        stops (calc-stops trip)
        first-stop (first stops)

        processed-trip (assoc (dissoc trip :pickup-datetime :dropoff-datetime :pickup-lat
                                    :pickup-lng :dropoff-lat :dropoff-lng) :datetime (first-stop :datetime)
                                                                           :geom (first-stop :geom))

        ; res (merge row-as-hash {:stops stops})
        ]
        ; (println (drop 1 stops) processed-trip)
    (geomesa/write-trip->kafka processed-trip (drop 1 stops))
      ; (send-to-kafka res)
      ))

; (defn -main
;   [& args]
;   (let [port (Integer/parseInt (or (env :port) "8080"))]
;     (info "Server running on port" port)
;     (run-server handler {:port port})))
