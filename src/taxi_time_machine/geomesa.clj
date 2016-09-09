(ns taxi-time-machine.geomesa
  (:import  (java.util Date)
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

; kafka-broker-param "brokers" "localhost:9092"
; zookeepers-param "zookepers" "zoo1:2181"
; zk-path "zkPath" "/geomesa/ds/kafka"
; kafka-connection-params ["brokers" "zookeepers" "zkpath"]
;
; kafka-datastore-conf
; {"brokers" "localhost:9092"
; "zookeepers" "zoo1:2181"
; "zkPath" "/geomesa/ds/kafka"
; "automated" "automated"}

(defn add-simple-features
  "add a SimpleFeature to the producer every half second"
  [^SimpleFeatureType sft ^FeatureStore producerfs]
  (let [min-x -180
        max-x 180
        min-y -90
        max-y 90
        dx 2
        dy 1
        people-names ["James", "John", "Peter", "Hannah", "Claire", "Gabriel"]
        seconds-per-year (* 365 24 60 60)
        random (rand)
        min-date (DateTime. 2015, 1, 1, 0, 0, 0, (DateTimeZone/forID "UTC"))
        builder (SimpleFeatureBuilder. sft)
        feature-collection (DefaultFeatureCollection.)
        num-features (/ (- max-x min-x) dx)]

        ; creates and updates two SimpleFeatures.
        ; the first time this for loop runs the two SimpleFeatures are created.
        ; in the subsequent iterations of the for loop, the two SimpleFeatures are updated.
        (loop [i num-features]
          (.add builder (nth people-names (mod i (count people-names))))
          (.add builder (int (Math/round (* (rand) 110))))
          (.add builder (.toDate (.plusSeconds min-date (int (Math/round (* (rand) seconds-per-year))))))
          (.add builder (.read WKTUtils$/MODULE$ (str "POINT(" (* (+ min-x dx) i) " " (* (+ min-y dx) i) ")")))
          (let [feature1 (.buildFeature builder "1")]
            (.add builder (nth people-names (mod (+ i 1) (count people-names))))
            (.add builder (int (Math/round (* (rand) 110))))
            (.add builder (.toDate (.plusSeconds min-date (int (Math/round (* (rand) seconds-per-year))))))
            (.add builder (.read WKTUtils$/MODULE$ (str "POINT(" (* (+ min-x dx) i) " " (* (+ max-y dx) i) ")")))
            (let [feature2 (.buildFeature builder "2")]

              ; write the simplefeatures to kafka
              (.add feature-collection feature1)
              (.add feature-collection feature2)
              (.addFeatures producerfs feature-collection)
              (.clear feature-collection)

              ; wait 100 ms in between updating SimpleFeatures to simulate a stream of data
              (Thread/sleep 100))))))



  (defn add-delete-new-feature
    [^SimpleFeatureType sft ^FeatureStore producerfs]
    (let [builder (SimpleFeatureBuilder. sft)
          feature-collection (DefaultFeatureCollection.)
          identifier "1000"]
      (.add builder "Antoniuzzs")
      (.add builder (int (Math/round (* (rand) 110))))
      (.add builder (Date.))
      (.add builder (.read WKTUtils$/MODULE$ "POINT(-1 -1)"))
      (let [feature (.buildFeature builder identifier)]
        (.add feature-collection feature)
        (.addFeatures producerfs feature-collection)
        (let [^FilterFactory2 ff (CommonFactoryFinder/getFilterFactory2)
              ^Filter id-filter (.id ff (into-array [(.featureId ff identifier)]))]
          (.removeFeatures producerfs id-filter)))))

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
    [^String[] args]
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
      (let [sft-name "KafkaQuickStart"
            sft-schema "name:String,age:Int,dtg:Date,*geom:Point:srid=4326"
            sft (SimpleFeatureTypes/createType sft-name sft-schema)
            ; set zkPath to default if not specified
            zk-path (or (ds-conf "zkPath") "/geomesa/ds/kafka")
            prepped-output-sft (KafkaDataStoreHelper/createStreamingSFT sft zk-path)
            ; only create the schema if it hasn't been created already
            ; x (if (not (contains? (vector (.getTypeNames producer-ds)) sft-name)) (.createSchema producer-ds prepped-output-sft))
            ; the live consumer must be created before the producer writes features
            ; in order to read streaming data.
            ; i.e. the live consumer will only read data written after its instantiation
            consumer-fs (.getFeatureSource consumer-ds sft-name)
            producer-fs (.getFeatureSource producer-ds sft-name)
            ]
      ; creates and adds SimpleFeatures to the producer every 1/5th of a second
      (println "Writing features to Kafka... refresh GeoServer layer preview to see changes")
      (let [replay-start (Instant.)]
        (add-simple-features sft producer-fs)
        (let [replay-end (Instant.)]

        ; read from Kafka after writing all the features.
        ; LIVE CONSUMER - will obtain the current state of SimpleFeatures
        (println "\nConsuming with the live consumer...")
        (let [feature-collection (.getFeatures consumer-fs)]
          (println (str (.size feature-collection) " features were written to Kafka"))

          (add-delete-new-feature sft producer-fs)


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
                (print-feature feature2)

            ; REPLAY CONSUMER - will obtain the state of SimpleFeatures at any specified time
            ; Replay consumer requires a ReplayConfig which takes a time range and a
            ; duration of time to process
            (println "\nConsuming with the replay consumer...")
            (let [read-behind (Duration. 1000); 1 second readBehind
                  rc (ReplayConfig. replay-start replay-end read-behind)
                  replay-sft (KafkaDataStoreHelper/createReplaySFT prepped-output-sft rc)]
              (.createSchema producer-ds replay-sft)
              (let [replay-consumer-fs (.getFeatureSource consumer-ds (.getName replay-sft))
                    ; querying for the state of SimpleFeatures approximately 5 seconds before the replay-end.
                    ; the ReplayKafkaConsumerFeatureSource will build the state of SimpleFeatures
                    ; by processing all of the messages that were sent in between queryTime-readBehind and queryTime.
                    ; only the messages in between replay-start and replay-end are cached.
                    query-time (.minus replay-end 5000)
                    feature-collection (.getFeatures replay-consumer-fs (ReplayTimeHelper/toFilter query-time))]

                (println (str (.size feature-collection) " features were written to Kafka"))
                (println "Here are the two SimpleFeatures that were obtained with the replay consumer:")

                (let [feature-iterator (.features feature-collection)
                      feature1 (.next feature-iterator)
                      feature2 (.next feature-iterator)]
                      (.close feature-iterator)
                      (print-feature feature1)
                      (print-feature feature2)

                  (if (not (nil? (System/getProperty "clear")))
                    (.removeFeatures producer-fs (.INCLUDE Filter))
                  ))))))))))))
