(ns taxi-time-machine.core
  (:require [org.httpkit.server :refer [run-server]]
            [environ.core :refer [env]]
            [taoensso.timbre :as timbre]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [clojure.java.io :as io])
  (:gen-class))

(timbre/refer-timbre)

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp "resources/public/index.html")})

(defn -main
  [& args]
  (let [port (Integer/parseInt (or (env :port) "8080"))]
    (info "Server running on port" port)
    (run-server handler {:port port})))
