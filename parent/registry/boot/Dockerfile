# Multi-stage Dockerfile to build and run the RegistryApplication
# Stage 1: build using Maven + JDK 17
FROM maven:3.9.4-eclipse-temurin-17 AS builder

WORKDIR /workspace

# Copy the whole repository to allow building multi-module with -pl -am
COPY . /workspace

# Build only the registry-boot module and its required modules (skip tests to speed up)
RUN mvn -B -DskipTests -pl parent/registry/boot -am package

# Stage 2: runtime image with JRE 17
FROM eclipse-temurin:17-jre

# Copy the built jar from the builder stage. The artifact produced by the module is expected
# to be under parent/registry/boot/target/ and follow the pattern registry-boot-*.jar
ARG JAR_GLOB=/workspace/parent/registry/boot/target/registry-boot-*.jar
COPY --from=builder ${JAR_GLOB} /app/app.jar

EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

