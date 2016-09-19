# taxi-time-machine

Go back in time to see what NYers are really up to...

### Status: Working in progress

Prerequisites
-------------
* [OSRM](https://github.com/Project-OSRM/osrm-backend) or other routing API
* basic knowledge of [GeoTools](http://www.geotools.org), [GeoServer](http://geoserver.org), and Kafka
* an instance of Kafka 0.8.2.x with (an) appropriate Zookeeper instance(s)
* an instance of GeoServer version 2.8.1 with the GeoMesa Kafka plugin installed
* [Java JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Apache Maven](http://maven.apache.org/) 3.2.2 or better
* a [git](http://git-scm.com/) client

In order to install the GeoMesa Kafka GeoServer plugin, follow the instructions
[here](https://github.com/locationtech/geomesa/tree/master/geomesa-gs-plugin/geomesa-kafka-gs-plugin).

Ensure your Kafka and Zookeeper instances are running. You can use Kafka's
[quickstart](http://kafka.apache.org/documentation.html#quickstart) to get Kafka/Zookeeper
instances up and running quickly.
