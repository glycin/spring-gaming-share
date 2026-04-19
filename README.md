# spring-game

Repository containing the code shown for the talk **"Let's use Spring Boot to build games, because why not"** by Alexander Chatzizacharias.

This repository is intended for public sharing, thus doesn't contain art/audio that has restrictive licenses and differs what is shown during the talk. 
The code is the same.

Modules share this git repo but are **not** a Gradle composite build: every module has its own `build.gradle.kts` and `gradlew`.

## Requirements

- JDK 24
- Docker + Docker Compose (only for the observability stack)
- macOS / Linux / Windows

## Modules

- `shared/`
- `spring-pong/`
- `spring-snake/`
- `spring-shooter/`
- `spring-aria/`
- `spring-souls/`
- `spring-survivors/`

## Running a module

Each module is standalone. `cd` into it and use its own wrapper:

```bash
cd spring-<module>
./gradlew bootRun
```

A Swing window or AWT game canvas opens when the context finishes loading.

### `spring-souls` (LWJGL / OpenGL)

LWJGL requires two extra JVM flags. Both are wired into `spring-souls/build.gradle.kts` so `./gradlew bootRun` works out of the box:

- `--enable-native-access=ALL-UNNAMED` — silences the Java 24 restricted-native-access warning.
- `-XstartOnFirstThread` — **required on macOS**. GLFW must own the main thread or the window never opens. Added automatically for macOS hosts.

## Observability stack (Docker)

A single `docker-compose.yml` at the repo root provides OTel Collector → Prometheus → Grafana. Modules that export metrics (`spring-souls`, `spring-survivors`) send OTLP to `localhost:4318`.

### Start

```bash
docker compose up -d
```

### Services

| Service        | URL                         | Notes                                 |
| -------------- | --------------------------- | ------------------------------------- |
| OTel Collector | `http://localhost:4318`     | OTLP HTTP receiver (modules post here)|
| OTel metrics   | `http://localhost:8889`     | Prometheus exporter                   |
| Prometheus     | `http://localhost:9091`     | Host port 9091 → container 9090       |
| Grafana        | `http://localhost:3000`     | Dashboards auto-provisioned           |

Grafana login: `glycin` / `123` (anonymous viewer access is also enabled).

Dashboards are provisioned from `grafana/dashboards/` (e.g. `spring-souls.json`, `spring-survivors.json`). Datasource wiring lives in `grafana/provisioning/`. OTel Collector config: `otel/otel-collector-config.yaml`.

### Thanks

Thanks for taking a look at this goofy project. Hope you have fun with it!