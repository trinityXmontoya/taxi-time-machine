(defproject taxi-time-machine "0.1.0-SNAPSHOT"
  :description "See README"
  :url "https://github.com/trinityXmontoya/taxi-time-machine"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring "1.4.0"]
                 [http-kit "2.1.18"]
                 [environ "1.0.3"]
                 [com.taoensso/timbre "4.3.1"]
                 [cheshire "5.6.3"]
                 [clj-kafka "0.3.4"]
                 [clojure-csv/clojure-csv "2.0.1"]
                 [org.clojure/data.csv "0.1.3"]
                 [clj-time "0.12.0"]
                 [org.locationtech.geomesa/geomesa-kafka-datastore "1.2.5"
                    :exclusions [[org.scala-lang.modules/scala-xml_2.11]
                                  [org.scala-lang/scala-library]
                                  [org.scala-lang.modules/scala-parser-combinators_2.11]
                                  [com.typesafe.scala-logging/scala-logging_2.11]]]
                ;  [org.locationtech.geomesa/geomesa-utils "1.2.5"
                ;   :exclusions [[org.scala-lang.modules/scala-parser-combinators_2.11]
                ;                [org.scala-lang/scala-library]
                ;                [com.typesafe.scala-logging/scala-logging_2.11]]]
                ;  [org.geotools/gt-data "14.1"]
                ;  [org.geotools/gt-wfs "14.1"]
                ;  [org.geotools/gt-epsg-wkt "14.1"]
                ;  [org.geotools/gt-epsg-hsql "14.1"]
                ;  [org.geotools/gt-opengis "14.1"]

                 ]
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.0.3"]
            [lein-localrepo "0.5.3"]]
  :ring {:handler taxi-time-machine.core/handler}
  :main ^:skip-aot taxi-time-machine.core
  :target-path "target/%s"
  ; :repositories [["local" ~(str (.toURI (java.io.File. "maven_repo")))]]
  :repositories {"locationtech-releases" "https://repo.locationtech.org/content/groups/releases"
                 "geomesa-snapshots" "https://repo.locationtech.org/content/repositories/geomesa-snapshots"
                 "boundlessgeo" "http://repo.boundlessgeo.com/main"
                 "osgeo" "http://download.osgeo.org/webdav/geotools"
                 "conjars.org" "http://conjars.org/repo"}
  :profiles {:uberjar {:aot :all}})


  ; package com.example.geomesa.kafka;
  ;
  ; import org.apache.commons.cli.*;
  ; import org.geotools.data.DataStore;
  ; import org.geotools.data.DataStoreFinder;
  ; import org.geotools.data.FeatureStore;
  ; import org.joda.time.DateTime;
  ; import org.joda.time.DateTimeZone;
  ; import org.joda.time.Duration;
  ; import org.joda.time.Instant;

    ; import java.io.IOException;
    ; import java.util.*;

  ; import org.geotools.data.simple.SimpleFeatureCollection;
  ; import org.geotools.data.simple.SimpleFeatureIterator;
  ; import org.geotools.data.simple.SimpleFeatureSource;
  ; import org.geotools.data.simple.SimpleFeatureStore;
  ; import org.geotools.factory.CommonFactoryFinder;
  ; import org.geotools.feature.DefaultFeatureCollection;
  ; import org.geotools.feature.simple.SimpleFeatureBuilder;
  ;
  ;
  ;
  ; xxx
  ; ; import org.locationtech.geomesa.kafka.KafkaDataStoreHelper;
  ; ; import org.locationtech.geomesa.kafka.ReplayConfig;
  ; ; import org.locationtech.geomesa.kafka.ReplayTimeHelper;
  ; ; import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes;
  ; ; import org.locationtech.geomesa.utils.text.WKTUtils$;
  ;
  ; import org.opengis.feature.Property;
  ; import org.opengis.feature.simple.SimpleFeature;
  ; import org.opengis.feature.simple.SimpleFeatureType;
  ; import org.opengis.feature.type.Name;
  ; import org.opengis.filter.Filter;
  ; import org.opengis.filter.FilterFactory2;
