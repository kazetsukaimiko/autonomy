# Autonomy Restructure

This document outlines the basic process for autonomy's restructure, as I've made quite the mess of things.


## Basis
Autonomy in its current state is monolithic and difficult to maintain. The monolithic nature in particular makes 
the application unreliable over longer periods of time and less resilient when degraded.

## Solutions

### Event Bus
Rather than the CDI event bus being the backend for message delivery it will be the delegator. The backend will be a 
message queue like Kafka or MQTT.

### One concern, one deployment
Instead of JSONLink, Victron, etc all being in the same application, these will become individual deployments that talk
to one another over the event bus.

### Direct device allocation
USB device files will be passes as arguments to the application somehow, the application will not try to ascertain
which devices are what any longer.

### Docker and docker-compose
Rather than SystemD user units this will switch to docker/docker-compose. One concern, one image/container.

## Current Deployments / Containers
- jsonlink 
- victron
- daly
- speech : Simple service to invoke TTS commands
- web



