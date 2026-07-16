FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -Pproduction package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system jinmi \
    && useradd --system --gid jinmi --home-dir /app --shell /usr/sbin/nologin jinmi \
    && mkdir -p /app/uploads \
    && chown -R jinmi:jinmi /app
COPY --from=build --chown=jinmi:jinmi /app/target/jinmi-shop-*.jar app.jar
VOLUME ["/app/uploads"]
EXPOSE 8080
USER jinmi
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl --fail --silent http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-Djava.security.egd=file:/dev/urandom","-jar","app.jar","--spring.profiles.active=prod"]
