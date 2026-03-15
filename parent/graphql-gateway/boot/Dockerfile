# Multi-stage Dockerfile to build and run the GraphQL Gateway application
# Stage 1: build using Maven + JDK 21 (matching project toolchain)
FROM maven:3.9.4-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copy the whole repository to allow building multi-module with -pl -am
COPY . /workspace

# Build parent, graphql-gateway-service and graphql-gateway-boot in a single run using one BuildKit cache mount.
# This mirrors the registry build to maximise reuse of Maven dependencies across modules.
RUN --mount=type=cache,target=/root/.m2 /bin/bash -c '\
  set -euo pipefail; \
  echo "=== Building parent, parent/graphql-gateway/service and parent/graphql-gateway/boot in one reactor ==="; \
  mvn -B -T 1C -DskipTests -Dmaven.test.skip=true -Dskip.rabbitmq.tests=true -Dgpg.skip=true -pl parent,parent/graphql-gateway/service,parent/graphql-gateway/boot -am install; \
'

# Select the repackaged jar and copy it to a known location for the runtime stage
RUN /bin/bash -c '\
  set -euo pipefail; \
  echo "Looking for repackaged jar in parent/graphql-gateway/boot/target"; \
  ls -la parent/graphql-gateway/boot/target || true; \
  JAR=""; \
  for f in parent/graphql-gateway/boot/target/*.jar; do \
    case "$(basename "$f")" in \
      *-sources.jar|*-javadoc.jar) continue ;; \
      *.jar) JAR="$f"; break ;; \
    esac; \
  done; \
  if [ -z "$JAR" ]; then echo "No jar found in target" >&2; exit 1; fi; \
  echo "Selected jar: $JAR"; \
  cp -- "$JAR" /workspace/app.jar; \
'

# Stage 2: runtime image with JRE 21
FROM eclipse-temurin:21-jre

WORKDIR /app

# Install curl for simple healthchecks / debugging (optional)
RUN apt-get update && apt-get install -y curl ca-certificates --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

# Copy the built jar from the builder stage. The artifact produced by the module is expected
# to be under /workspace/app.jar
COPY --from=builder /workspace/app.jar /app/app.jar

EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

