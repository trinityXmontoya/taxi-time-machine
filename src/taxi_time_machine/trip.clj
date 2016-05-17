(ns taxi-time-machine.trip
  (:require [taxi-time-machine.pg :as pg]
            [taoensso.timbre :as timbre]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/trips.sql" {:connection @pg/conn})

(defn build-polygon-str
  [bbox]
  (let [first-pair (first bbox)]
    (str "POLYGON(("
         (clojure.string/join "," (map #(str (second %) " "  (first %)) bbox))
         (str "," (second first-pair) " " (first first-pair))
         "))")))

(defn get-trips [datetime bbox limit]
  (let [datetime "2014-01-04 19:17:05"
        bbox [[40.7753, -73.9638],[40.7753, -73.9560],[40.7703, -73.9560] [40.7703, -73.9638]]
        polygon (build-polygon-str bbox)]
    (query-trip-by-datetime-range {:datetime datetime
                                   :bbox_polygon polygon
                                   :limit (or limit 10)})))
