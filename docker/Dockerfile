ARG SERVICE_NAME

# ── Build Stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build
ARG SERVICE_NAME

WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && \
    ./gradlew :${SERVICE_NAME}:build -x test --no-daemon && \
    find /app/${SERVICE_NAME}/build/libs -name "*.jar" ! -name "*plain*" \
         -exec cp {} /app/app.jar \;

# ── Runtime Stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]