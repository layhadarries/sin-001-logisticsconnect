# HubServiceApp

## Overview

Serves provinces and sorting centers (place-name source of truth).

Part of the [LogisticsConnect](../README.md) project. Independent Maven module, no
parent pom.

## Project structure

```
hub-service/
├── pom.xml
└── src/main/java/co/wethinkcode/logisticsconnect/
        ├── Hub.java
        └── HubServiceApp.java
```

## Build

```
mvn package
```

## Run

```
java -jar target/hub-service.jar
```

Listens on port `7051`. On startup, fetches the cleaned hub list from
ingestion-service (`GET :7050/hubs`) and caches it in memory. If
ingestion-service isn't reachable yet, hub-service starts anyway with an empty
list and logs why — restart it once ingestion-service is up.

### Endpoints

| Method | Path | Returns |
|---|---|---|
| GET | `/health` | `OK` |
| GET | `/hubs` | JSON array of all cached hubs |
| GET | `/hubs/{hubId}` | JSON object for one hub, or `404` if unknown |

## Test

**ingestion-service must already be running** (port `7050`) before starting
hub-service, since it loads its data from there on startup.

```
curl http://localhost:7051/health          # -> OK
curl http://localhost:7051/hubs            # -> JSON array of hubs
curl http://localhost:7051/hubs/H-500      # -> JSON object for H-500
```

To add real tests, add JUnit 5 + the Surefire plugin to `pom.xml`, put tests under
`src/test/java/co/wethinkcode/logisticsconnect/`, and run `mvn test`.
