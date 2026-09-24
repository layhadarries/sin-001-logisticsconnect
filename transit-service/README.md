# TransitServiceApp

## Overview

Calculates estimated arrival windows based on hub and delay stage.

Part of the [LogisticsConnect](../README.md) project. Independent Maven module, no
parent pom.

MQ: this service subscribes to the ActiveMQ topic `package-status-topic` — see [`../common/`](../common). Broker URL and topic name come from the common `co.wethinkcode.logisticsconnect.mq.MqConfig` class alongside it in this module.

## Project structure

```
transit-service/
├── pom.xml
└── src/main/java/co/wethinkcode/logisticsconnect/
    ├── TransitServiceApp.java
    ├── Hub.java
    ├── TransitEstimate.java
    ├── StageUpdateMessage.java
    └── mq/
        └── MqConfig.java
```

## Build

```
mvn package
```

## Run

```
java -jar target/transit-service.jar
```

Listens on port `7053`. On startup, subscribes to the `package-status-topic`
ActiveMQ topic — the broker (`../common/`) must already be running. Calls
hub-service (`:7051`) directly over REST for hub/location data; the delay
stage comes only from the MQ subscription, not a direct call to
delay-stage-service.

### Endpoints

| Method | Path | Returns |
|---|---|---|
| GET | `/health` | `OK` |
| GET | `/transit/{hubId}` | Estimated transit time for a hub, or `404` if the hub doesn't exist |

`estimatedHours = 24 + (delayStage * 2)`, where `delayStage` is whatever this
service last received over `package-status-topic` for that hub (defaults to
`0` if no update has arrived yet).

## Test

**Start order matters here** — this service needs the broker, hub-service, and
(to see a non-zero delay stage) delay-stage-service all already running:

```
# 1. broker
cd /common && docker compose up -d
 
# 2. dependencies, each in its own terminal
cd /hub-service && mvn package && java -jar target/hub-service.jar
cd /delay-stage-service && mvn package && java -jar target/delay-stage-service.jar
 
# 3. this service
mvn package && java -jar target/transit-service.jar
```

Then:

```
curl http://localhost:7053/health          # -> OK
curl http://localhost:7053/transit/H-999   # -> 404, unknown hub
 
curl -X POST http://localhost:7052/delay-stage/H-500 \
  -H "Content-Type: application/json" -d '{"stage": 5}'
 
curl http://localhost:7053/transit/H-500
# -> {"hubId":"H-500","sortingCenter":"Johannesburg Central","province":"Gauteng","delayStage":5,"estimatedHours":34}%    
```

Watch this service's own console while you POST to delay-stage-service — it
prints `[transit-service] received stage update: ...` the moment the message
arrives over the topic, which is the proof the MQ link (not a REST call) is
what updated the ETA.


To add real tests, add JUnit 5 + the Surefire plugin to `pom.xml`, put tests under
`src/test/java/co/wethinkcode/logisticsconnect/`, and run `mvn test`.
