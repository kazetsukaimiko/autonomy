# autonomy
Autonomous Operations

## Overview
Autonomy is the automation software suite used in my bus conversion. It integrates:
* Victron Energy Device Data

  https://github.com/kazetsukaimiko/freedriver/tree/master/victron
* DIY Arcade (Joystick) input

  Using jstest and ProcessBuilder- somewhat platform dependent on Linux
* BMS data from Electrodacus SBMS0 

  https://github.com/kazetsukaimiko/freedriver/tree/master/electrodacus
* A Generic Arduino sketch for reading/writing to GPIO 

  https://github.com/kazetsukaimiko/freedriver/tree/master/jsonlink
* Services and models to create/track GPIO-to-appliance mappings 
* A from-scratch HTML/CSS/JS page to control appliances via REST
* Other stuff I can't remember

## How it works
In a nutshell I have:
* A DIY arcade joystick board wired into buttons on my walls
* An Arduino 2560 MEGA connected to two 16 channel relay controllers

Using Autonomy I create a mapping from GPIO to appliance (Ardunio Digital pins), or GPIO to sensor (Arduino Analog pins):
* 46 -> "laundry"
* 40 -> "hallway"

I then have a second pair of mappings from joystick:state to appliance which toggles those appliances:
* 11:0 -> "hallway"
* 10:0 -> "laundry"

So that when the Joystick button 11 (11) is released (0), that toggles GPIO 40.

![](/readme/button_action.gif)

Additionally the GPIO to appliance mappings allow for REST control:

![](/readme/remote.gif)


## Architecture
Autonomy is built on Java/Jakarta EE 8+, making use of JAX-RS, CDI, JPA2 among other technologies. Its history includes deployments on Wildfly, Wildfly Swarm, Thorntail.io and now Quarkus. 

Currently there are several modules:
* api - the JAX-RS APIs and data model specific to Autonomy 
* jpa - the JPA entities specific to Autonomy, generates a static metamodel used for typesafe CriteriaQueries
* quarkus - Quarkus deployment and all Java EE Service code.
* ui - is an ongoing attempt to experiment with FE technologies to formalize UI development and testing

Autonomy makes heavy use of my upstream freedriver project, tools which are not specific to the Autonomy use case.

https://github.com/kazetsukaimiko/freedriver/ 

The REST control video above is from a long time ago when to read pin states one had to ask the Arduino prior to issuing commands- since then I've placed an [Infinispan Cache](https://infinispan.org/) in front of GPIO control which has made the REST interface much more responsive. A neat side effect is concurrency related.... REST services that talk to serial create some interesting concurrency issues, which the cache alleviates.

Internally Autonomy is quite event driven, although not as much as it could be. Instead of binding joystick reads to action, they are sent using the CDI Event bus and received by services classes using @Observes. Currently there is only one service with several REST APIs, although I hope to move to microservices, see below.

## Future plans
With the migration to Quarkus, the goal is to break up Autonomy as a monolithic web application into several microservices, using a message bus/service rather than vanilla CDI events:
* Joystick MicroService - Takes a configuration file, starts processes to read event data from specified joystick devices and spawn events to send over message bus.
* VEDirect MicroService - Takes a confuration file, starts threads to read serial data from Victron VEDirect devices, spawn events to send over message bus.
* BMS MicroService - Takes a configration file, starts threads to read serial data from DALY Smart BMS, spawn events to send over message bus.
* JSONLink MicroService - Takes a configuration file for Arduino boards to listen to- spawns jsonlink.Response events, listens for jsonlink.Request events on the message bus
* Mapping MicroService -  Takes configuration for GPIO to Appliance mappings- listens on various message bus topics to take action based on those mappings, provides REST interface for high-level control of devices

Message bus interaction should be agnostic of the implementation- ideally continue use of CDI Event interfaces or JMS. 

Additionally there are items not captured in this repository- by default the application uses H2 database but for my purposes I actually use MySQL. Logstash then moves event data from all event sources to ElasticSearch / Kibana. An effort is being made to pull this environment into VCS through Dockerfiles and confd. Kibana is uses mostly ask visual questions about lithium cell health.
