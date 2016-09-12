(ns taxi-time-machine.geomesa
  (:import  (java.util Date Arrays)
            (org.joda.time  DateTime
                            DateTimeZone
                            Duration
                            Instant)
            (org.geotools.data DataStore
                                DataStoreFinder
                                FeatureStore)
             (org.geotools.data.simple SimpleFeatureCollection
                                       SimpleFeatureIterator
                                       SimpleFeatureSource
                                       SimpleFeatureStore)
             (org.geotools.factory CommonFactoryFinder)
             (org.geotools.feature DefaultFeatureCollection)
             (org.geotools.feature.simple SimpleFeatureBuilder)
             (org.locationtech.geomesa.kafka KafkaDataStoreHelper
                                             ReplayConfig
                                             ReplayTimeHelper)
             (org.locationtech.geomesa.utils.geotools SimpleFeatureTypes)
             (org.locationtech.geomesa.utils.text WKTUtils$)
             (org.opengis.feature Property)
             (org.opengis.feature.simple SimpleFeature
                                         SimpleFeatureType)
             (org.opengis.feature.type Name)
             (org.opengis.filter Filter
                                 FilterFactory2))
  (:gen-class))

(def kafka-datastore-conf
  {"brokers" "localhost:9092"
  "zookeepers" "localhost:2181"
  "zkPath" "/geomesa/ds/kafka"
  "automated" "automated"})

(def trip-schema
  (str "vendor-id:Int,"
       "pickup-datetime:Date,"
       "dropoff-datetime:Date,"
       "passenger-count:Int,"
       "trip-dist:Double,"
       "pickup-coords:Point:srid=4326,"
       "rate-code-id:Int,"
       "store-and-fwd-flag:String,"
       "dropoff-coords:Point:srid=4326,"
       "payment-type:Int,"
       "fare-amt:Double,"
       "extra:Double,"
       "mta-tax:Double,"
       "tip-amt:Double,"
       "tolls-amt:Double,"
       "total-amt:Double"))

(defn build-simple-feature
  "build SimpleFeature"
  [^SimpleFeatureType sft obj]
  (let [builder (SimpleFeatureBuilder. sft)
        pickup-coords (.read WKTUtils$/MODULE$ (str "POINT(" (:pickup-lat obj) " " (:pickup-lng obj) ")"))
        dropoff-coords (.read WKTUtils$/MODULE$ (str "POINT(" (:pickup-lat obj) " " (:pickup-lng obj) ")"))
        obj (dissoc obj :pickup-lat :pickup-lng :dropoff-lat :dropoff-lng)]
      (doseq [attr obj]
        (.set builder (name (key attr)) (val attr)))
      (.set builder "pickup-coords" pickup-coords)
      (.set builder "dropoff-coords" dropoff-coords)
    (.buildFeature builder nil)))

(defn add-simple-feature
  "add a SimpleFeature to the producer"
  [^SimpleFeatureType sft
   ^FeatureStore producer-fs
   obj]
  (let [sf (build-simple-feature sft obj)
        feature-collection (DefaultFeatureCollection.)]
        (println "iamthefeature" sf)
    (.add feature-collection sf)
    (.addFeatures producer-fs feature-collection)
    (.clear feature-collection)))

(defn print-feature
  "prints out attribute values for a SimpleFeature"
  [^SimpleFeature f]
  (let [props (.iterator (.getProperties f))
        prop-count (.getAttributeCount f)]
        (println (str "fid: " (.getID f)))
        (loop [i prop-count]
          (let [prop-name (.getName (.next props))]
            (println (str " | " prop-name ":" (.getAttribute f prop-name)))))))


  (defn -main
    ; [^String[] args]
    []
    (let [ds-conf {"brokers" "localhost:9092"
                   "zookeepers" "localhost:2181"
                   "zkPath" "/geomesa/ds/kafka"
                   "automated" "automated"}
          producer-ds (DataStoreFinder/getDataStore (merge ds-conf {"isProducer" true}))
          consumer-ds (DataStoreFinder/getDataStore (merge ds-conf {"isProducer" false}))]

      ; verify that we got back our KafkaDataStore objects properly
      (if (nil? producer-ds)
        (throw "Null producer KafkaDataStore"))
      (if (nil? consumer-ds)
        (throw "Null consumer KafkaDataStore"))

      ; create the schema which creates a topic in Kafka
      ; (only needs to be done once)
      (let [sft-name "KafkaQuickStartClojureTest"
            sft-schema trip-schema
            sft (SimpleFeatureTypes/createType sft-name sft-schema)
            ; set zkPath to default if not specified
            zk-path (or (ds-conf "zkPath") "/geomesa/ds/kafka")
            prepped-output-sft (KafkaDataStoreHelper/createStreamingSFT sft zk-path)
            ; only create the schema if it hasn't been created already
            x (if (not (.contains (Arrays/asList (.getTypeNames producer-ds)) sft-name)) (.createSchema producer-ds prepped-output-sft))
            ; the live consumer must be created before the producer writes features
            ; in order to read streaming data.
            ; i.e. the live consumer will only read data written after its instantiation
            consumer-fs (.getFeatureSource consumer-ds sft-name)
            producer-fs (.getFeatureSource producer-ds sft-name)
            samples [{:tolls-amt "0", :pickup-lng "-73.981498718261719", :mta-tax "0.5", :store-and-fwd-flag "N", :extra "0.5", :dropoff-lat "40.782432556152344", :rate-code-id "1", :trip-dist "1.20", :pickup-lat "40.771186828613281", :dropoff-datetime #inst "2015-01-01T00:27:07.000-00:00", :passenger-count "1", :tip-amt "0", :dropoff-lng "-73.972816467285156", :fare-amt "7", :payment-type "2", :total-amt "8.3", :pickup-datetime #inst "2015-01-01T00:20:41.000-00:00", :vendor-id "1"}
            {:tolls-amt "0.3", :pickup-lng "-74.981498718261719", :mta-tax "0.5", :store-and-fwd-flag "N", :extra "0.5", :dropoff-lat "40.782432556152344", :rate-code-id "1", :trip-dist "1.20", :pickup-lat "40.771186828613281", :dropoff-datetime #inst "2015-01-02T00:27:07.000-00:00", :passenger-count "1", :tip-amt "0", :dropoff-lng "-74.972816467285156", :fare-amt "7", :payment-type "2", :total-amt "8.3", :pickup-datetime #inst "2015-01-02T00:20:41.000-00:00", :vendor-id "1"}]]
      ; creates and adds SimpleFeatures to the producer every 1/5th of a second
      (println "Writing features to Kafka... refresh GeoServer layer preview to see changes")
      (mapv #(add-simple-feature sft producer-fs %) samples)





      ; (let [replay-start (Instant.)]
      ;   (add-simple-features sft producer-fs)
      ;   (let [replay-end (Instant.)]
      ;
      ;   ; read from Kafka after writing all the features.
      ;   ; LIVE CONSUMER - will obtain the current state of SimpleFeatures
      ;   (println "\nConsuming with the live consumer...")
      ;   (let [feature-collection (.getFeatures consumer-fs)]
      ;     (println (str (.size feature-collection) " features were written to Kafka"))
      ;
      ;     (add-delete-new-feature sft producer-fs)


          ; read from Kafka after writing all the features.
          ; LIVE CONSUMER - will obtain the current state of SimpleFeatures
          (println "\nConsuming with the live consumer...")
          (let [feature-collection (.getFeatures consumer-fs)]
            (println (str (.size feature-collection) " features were written to Kafka"))

          ; the state of the two SimpleFeatures is real time here
          (println "Here are the two SimpleFeatures that were obtained with the live consumer:")
          (let [feature-iterator (.features feature-collection)
                feature1 (.next feature-iterator)
                feature2 (.next feature-iterator)]
                (.close feature-iterator)
                (print-feature feature1)
                (print-feature feature2))))))


            ;
            ; ; REPLAY CONSUMER - will obtain the state of SimpleFeatures at any specified time
            ; ; Replay consumer requires a ReplayConfig which takes a time range and a
            ; ; duration of time to process
            ; (println "\nConsuming with the replay consumer...")
            ; (let [read-behind (Duration. 1000); 1 second readBehind
            ;       rc (ReplayConfig. replay-start replay-end read-behind)
            ;       replay-sft (KafkaDataStoreHelper/createReplaySFT prepped-output-sft rc)]
            ;   (.createSchema producer-ds replay-sft)
            ;   (let [replay-consumer-fs (.getFeatureSource consumer-ds (.getName replay-sft))
            ;         ; querying for the state of SimpleFeatures approximately 5 seconds before the replay-end.
            ;         ; the ReplayKafkaConsumerFeatureSource will build the state of SimpleFeatures
            ;         ; by processing all of the messages that were sent in between queryTime-readBehind and queryTime.
            ;         ; only the messages in between replay-start and replay-end are cached.
            ;         query-time (.minus replay-end 5000)
            ;         feature-collection (.getFeatures replay-consumer-fs (ReplayTimeHelper/toFilter query-time))]
            ;
            ;     (println (str (.size feature-collection) " features were written to Kafka"))
            ;     (println "Here are the two SimpleFeatures that were obtained with the replay consumer:")
            ;
            ;     (let [feature-iterator (.features feature-collection)
            ;           feature1 (.next feature-iterator)
            ;           feature2 (.next feature-iterator)]
            ;           (.close feature-iterator)
            ;           (print-feature feature1)
            ;           (print-feature feature2)
            ;
            ;       (if (not (nil? (System/getProperty "clear")))
            ;         (.removeFeatures producer-fs (.INCLUDE Filter)))
            ;       (System/exit 0))))))))))))
