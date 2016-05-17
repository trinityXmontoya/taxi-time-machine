(ns taxi-time-machine.pg
  (:require [clojure.java.jdbc :as jdbc]
            [environ.core :refer [env]])
           (:import com.zaxxer.hikari.HikariDataSource))

(defn get-pg-config
[]
(let [{:keys [pg-subname pg-user pg-pw]} env
     conf {:classname "org.postgresql.Driver"
           :subprotocol "postgresql"
           :subname pg-subname
          ;  :user pg-user
          ;  :password pg-pw
           }]
           conf))

(defn- pg-conn-pool
"Return a connection pool."
[spec]
(let [partitions (spec :partitions)
      min-pool-size (spec :min-pool-size)
      max-pool-size (spec :max-pool-size)
      cpds (doto (HikariDataSource.)
             (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
            ;  (.setUsername (:user spec))
            ;  (.setPassword (:password spec))

             )]
  {:datasource cpds}))

(def conn (-> (get-pg-config)
              (pg-conn-pool)
              (delay)))
