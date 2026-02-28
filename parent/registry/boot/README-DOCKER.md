# Running RegistryApplication with Docker

This document explains how to build and run the `registry-boot` module inside Docker using the provided multi-stage Dockerfile and docker-compose.

Prerequisites
- Docker (Engine and CLI)
- Docker Compose
- A working Maven build environment if you prefer to build the jar locally instead of using the multi-stage build

Build & Run (recommended - docker-compose)
From the repository root run:

```powershell
cd C:\repository\spring-base
docker-compose up --build -d
```

This will:
- build the `registry` image from `parent/registry/boot/Dockerfile`
- create the `middleware-net` bridge network
- run a container named `registry` with hostname `registry`
- map container port 8080 to host port 8080

Access
- The application will be available at: http://localhost:8080/
- Inside the `middleware-net` Docker network other services can reach the registry at `http://registry:8080`

Notes
- The Dockerfile performs a Maven build inside the image (multi-module build). If you want to speed up iteration you can build the jar locally and use a lightweight runtime image by copying the jar into the Dockerfile `/app`.
- If the build fails due to missing local modules, run `mvn -B -DskipTests -pl parent/registry/boot -am package` locally before `docker-compose up` to ensure artifacts are present.

