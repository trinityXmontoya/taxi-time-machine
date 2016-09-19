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
       "dtg:Date:index=true,"
       "passenger-count:Int,"
       "trip-dist:Double,"
       "rate-code-id:Int,"
       "store-and-fwd-flag:String,"
       "geom:Point:srid=4326:index=true,"
       "payment-type:Int,"
       "fare-amt:Double,"
       "extra:Double,"
       "mta-tax:Double,"
       "tip-amt:Double:index=true,"
       "tolls-amt:Double,"
       "total-amt:Double:index=true"))

(defn build-simple-feature
  "build SimpleFeature"
  [^SimpleFeatureType sft obj id]
  (let [builder (SimpleFeatureBuilder. sft)]
      (doseq [attr obj]
        (.set builder (name (key attr)) (val attr)))
    (.buildFeature builder id)))

(defn add-simple-feature
  "add a SimpleFeature to the producer"
  [^SimpleFeatureType sft
   ^FeatureStore producer-fs
   obj]
   (println "hereiam" obj)
  (let [sf (build-simple-feature sft obj)
        feature-collection (DefaultFeatureCollection.)]
        (println "simplefeature here" sf)
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



; conf

(def ds-conf {"brokers" "localhost:9092"
               "zookeepers" "localhost:2181"
               "zkPath" "/geomesa/ds/kafka"
               "automated" "automated"})
(def zk-path (or (ds-conf "zkPath") "/geomesa/ds/kafka"))
(def sft-name "KafkaQuickStartClojureTest12")
(def producer-ds (DataStoreFinder/getDataStore (merge ds-conf {"isProducer" true})))
(def consumer-ds (DataStoreFinder/getDataStore (merge ds-conf {"isProducer" false})))
(def sft (SimpleFeatureTypes/createType sft-name trip-schema))
(def prepped-output-sft (KafkaDataStoreHelper/createStreamingSFT sft zk-path))
(if (not (.contains (Arrays/asList (.getTypeNames producer-ds)) sft-name))
 (.createSchema producer-ds prepped-output-sft))
(def producer-fs (.getFeatureSource producer-ds sft-name))

(defn write-trip->kafka
  [trip points]
  (let [orig-feature (build-simple-feature sft trip nil)
        id (.getID orig-feature)
        feature-collection (DefaultFeatureCollection.)]
    (.add feature-collection orig-feature)
    (map (fn [point]
            (let [builder (SimpleFeatureBuilder. sft)]
              (.init builder orig-feature)
              (.set builder "geom" (point :geom))
              (.set builder "dtg" (point :dtg))
              (let [copy (.buildFeature builder id)]
                (.add feature-collection copy)))) points)
    (.addFeatures producer-fs feature-collection)
    (.clear feature-collection)))

(defn replay
  []
  (let [replay-begin (.toInstant (DateTime. #inst "2014-12-31T19:12:41"))
        replay-end (.toInstant (DateTime. #inst "2014-12-31T19:21:07"))
        read-behind (Duration. 1000); 1 second readBehind
        rc (ReplayConfig. replay-begin replay-end read-behind)
        replay-sft (KafkaDataStoreHelper/createReplaySFT prepped-output-sft rc)]
    (.createSchema producer-ds replay-sft)
    (let [replay-consumer-fs (.getFeatureSource consumer-ds (.getName replay-sft))
          ; querying for the state of SimpleFeatures approximately 5 seconds before the replay-end.
          ; the ReplayKafkaConsumerFeatureSource will build the state of SimpleFeatures
          ; by processing all of the messages that were sent in between queryTime-readBehind and queryTime.
          ; only the messages in between replay-start and replay-end are cached.
          query-time (.minus replay-end 10000)
          feature-collection (.getFeatures replay-consumer-fs (ReplayTimeHelper/toFilter query-time))]
      (println (str (.size feature-collection) " features were written to Kafka"))
      (println "Here are the two SimpleFeatures that were obtained with the replay consumer:")
      (let [feature-iterator (.features feature-collection)
           feature1 (.next feature-iterator)
           feature2 (.next feature-iterator)]
           (.close feature-iterator)
           (print-feature feature1)
           (print-feature feature2)))))
