# DelayStageServiceApp

## Overview

Tracks the Transit Delay Stage (0-8, e.g. weather shutdowns).

Part of the [LogisticsConnect](../README.md) project. Independent Maven module, no
parent pom.

MQ: this service publishes to the ActiveMQ topic `package-status-topic` — see [`../common/`](../common). Broker URL and topic name come from the common `co.wethinkcode.logisticsconnect.mq.MqConfig` class alongside it in this module.

## Project structure

```
delay-stage-service/
├── pom.xml
└── src/main/java/co/wethinkcode/logisticsconnect/
    ├── DelayStageServiceApp.java
    ├── StageUpdateRequest.java
    └── mq/
        └── MqConfig.java
```

## Build

```
mvn package
```

## Run

```
java -jar target/delay-stage-service.jar
```

Listens on port 7052. 
Connects to the ActiveMQ broker in ../common/ on startup 
— start the broker first (cd ../common && docker compose up -d).

### Endpoints

| Method | Path | Body | Returns |
|---|---|---|---|
| GET | `/health` | — | `OK` |
| GET | `/delay-stage/{hubId}` | — | `{"stage": N}` (defaults to `0` if never set) |
| POST | `/delay-stage/{hubId}` | `{"stage": N}`, `0 <= N <= 8` | `200` on success, `400` if out of range |

Every successful POST also publishes `{"hubId", "stage", "timestamp"}` to the
`package-status-topic` ActiveMQ topic — see [`../common/`](../common) — which
`transit-service` subscribes to instead of calling this service directly.

## Test

**The ActiveMQ broker must already be running** (`cd ../common && docker compose up -d`)
before starting this service.

```
curl http://localhost:7052/health                                             # -> OK
curl http://localhost:7052/delay-stage/H-500                                  # -> {"stage":0}
curl -X POST http://localhost:7052/delay-stage/H-500 \
  -H "Content-Type: application/json" -d '{"stage": 5}'
curl http://localhost:7052/delay-stage/H-500                                  # -> {"stage":5}
curl -X POST http://localhost:7052/delay-stage/H-500 \
  -H "Content-Type: application/json" -d '{"stage": 99}'                      # -> 400
```

To confirm the MQ publish is actually firing, watch this service's console output
while you POST — it prints `[delay-stage-service] published to topic: ...` on
every successful update.