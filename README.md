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
Internally Autonomy is quite event driven, although not as much as it could be. In a nutshell I have:
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
* api - the JAX-RS APIs and data model  specific to Autonomy 
* jpa - the JPA entities specific to Autonomy, generates a static metamodel used for typesafe CriteriaQueries
* quarkus - Quarkus deployment and all Java EE Service code.
* ui - is an ongoing attempt to experiment with FE technologies to formalize UI development and testing

Additionally, Autonomy makes heavy use of my upstream freedriver project, tools which are not specific to the Autonomy use case.

https://github.com/kazetsukaimiko/freedriver/

## Future plans
With the migration to Quarkus, the goal is to break up Autonomy as a monolithic web application into several microservices, using a message bus/service rather than vanilla CDI events:
* Joystick MicroService - Takes a configuration file, starts processes to read event data from specified joystick devices and spawn events to send over message bus.
* VEDirect MicroService - Takes a confuration file, starts threads to read serial data from Victron VEDirect devices, spawn events to send over message bus.
* BMS MicroService - Takes a configration file, starts threads to read serial data from DALY Smart BMS, spawn events to send over message bus.
* JSONLink MicroService - Takes a configuration file for Arduino boards to listen to- spawns jsonlink.Response events, listens for jsonlink.Request events on the message bus
* Mapping MicroService -  Takes configuration for GPIO to Appliance mappings- listens on various message bus topics to take action based on those mappings, provides REST interface for high-level control of devices

Message bus interaction should be agnostic of the implementation- ideally continue use of CDI Event interfaces or JMS. 

