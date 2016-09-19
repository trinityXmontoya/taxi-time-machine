(ns taxi-time-machine.core
  (:require [org.httpkit.client :as http]
            [environ.core :refer [env]]
            [taoensso.timbre :as timbre]
            [cheshire.core :as json]
            [taxi-time-machine.geomesa :as geomesa]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [clojure.java.shell :as shell]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io])
  (:import (org.locationtech.geomesa.utils.text WKTUtils$))
  (:gen-class))

(timbre/refer-timbre)

; date helpers
(def custom-formatter (f/formatter "yyyy-MM-dd HH:mm:ss"))
(defn ->datetime [str] (f/parse custom-formatter str))
(defn ->java-date [^org.joda.time.DateTime datetime] (c/to-date datetime))
(defn calc-trip-time [start end] (t/in-seconds (t/interval start end)))

; formatters
(defn row->hash
  [row]
  (let [fields [:vendor-id :pickup-datetime :dropoff-datetime :passenger-count
                :trip-dist :pickup-lng :pickup-lat :rate-code-id :store-and-fwd-flag
                :dropoff-lng :dropoff-lat :payment-type :fare-amt :extra :mta-tax
                :tip-amt :tolls-amt :total-amt]
       res (zipmap fields row)]
       (assoc res  :pickup-datetime (->datetime (res :pickup-datetime))
                   :dropoff-datetime (->datetime (res :dropoff-datetime)))))

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
                      {:geom point :dtg datetime})) coords)))

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

; (defn create-topic
;   [name]
;   (let [zk (kf-admin/zk-client "127.0.0.1:2181")]
;   (kf-admin/create-topic zk name)))
(defn clean-trip
  [trip stop]
  (assoc (dissoc trip :pickup-datetime :dropoff-datetime :pickup-lat
                      :pickup-lng :dropoff-lat :dropoff-lng) :dtg (stop :dtg)
                                                             :geom (stop :geom)))

(defn -main
  []
  (let [row ["1","2015-01-01 00:20:41","2015-01-01 00:27:07","1","1.20","-73.981498718261719","40.771186828613281","1","N","-73.972816467285156","40.782432556152344","2","7","0.5","0.5","0","0","8.3"]
        trip (row->hash row)
        stops (calc-stops trip)
        first-stop (first stops)
        processed-trip (clean-trip trip first-stop)
        ; rows (csv/read-csv (io/reader "yellow_tripdata_2015-01-06.csv"))
        ]
    ; (doseq [row rows]
    ;     (let [trip (row->hash row)
    ;           stops (calc-stops trip)
    ;           first-stop (first stops)
    ;           processed-trip (clean-trip trip first-stop)]
    ;       (geomesa/write-trip->kafka processed-trip (drop 1 stops))))

    (geomesa/write-trip->kafka processed-trip (drop 1 stops))
    (geomesa/replay)))
