# taxi-time-machine

Go back in time to see what NYers are really up to...


build geomesa, lein install




mvn deploy:deploy-file -Dfile=/Users/Alfred/Desktop/myfolders/code/DATADATA/geomesa/geomesa-kafka/geomesa-kafka-datastore/target/geomesa-kafka-datastore-1.2.6-SNAPSHOT.jar -Dpackaging=jar -Durl=file:maven_repo -DgroupId=org.locationtech.geomesa -DartifactId=geomesa-kafka-datastore -Dversion=1.2.6-SNAPSHOT -DpomFile=/Users/Alfred/Desktop/myfolders/code/DATADATA/geomesa/geomesa-kafka/geomesa-kafka-datastore/pom.xml

mvn deploy:deploy-file -Dfile=/Users/Alfred/Desktop/myfolders/code/DATADATA/geomesa/geomesa-kafka/geomesa-kafka-utils-common/target/geomesa-kafka-utils-common-1.2.6-SNAPSHOT.jar -Dpackaging=jar -Durl=file:maven_repo -DgroupId=org.locationtech.geomesa -DartifactId=geomesa-kafka-utils-common -Dversion=1.2.6-SNAPSHOT

mvn deploy:deploy-file -Dfile=/Users/Alfred/Desktop/myfolders/code/DATADATA/geomesa/geomesa-kafka/geomesa-kafka-08-utils/target/geomesa-kafka-08-utils-1.2.6-SNAPSHOT.jar -Dpackaging=jar -Durl=file:maven_repo -DgroupId=org.locationtech.geomesa -DartifactId=geomesa-kafka-08-utils -Dversion=1.2.6-SNAPSHOT


mvn deploy:deploy-file -Dfile=/Users/Alfred/Desktop/myfolders/code/DATADATA/geomesa-tutorials/geomesa-quickstart-kafka/target/geomesa-quickstart-kafka-1.2.5.1-SNAPSHOT.jar -Dpackaging=jar -Durl=file:maven_repo -DgroupId=com.example.geomesa -DartifactId=geomesa-quickstart-kafka -Dversion=1.2.5.1-SNAPSHOT -DpomFile=/Users/Alfred/Desktop/myfolders/code/DATADATA/geomesa-tutorials/geomesa-quickstart-kafka/pom.xml


lein localrepo install -r /Users/Alfred/Desktop/myfolders/code/DATADATA/geomesa-tutorials/geomesa-quickstart-kafka/target/geomesa-quickstart-kafka-1.2.5.1-SNAPSHOT.jar

lein localrepo coords /Users/Alfred/Desktop/myfolders/code/DATADATA/geomesa-tutorials/geomesa-quickstart-kafka/target/geomesa-quickstart-kafka-1.2.5.1-SNAPSHOT.jar | xargs lein localrepo install
