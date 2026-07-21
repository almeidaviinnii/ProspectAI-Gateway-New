FROM eclipse-temurin:17-jdk AS build

WORKDIR /src

COPY gradlew gradle.properties ./
COPY gradle/ ./gradle/
COPY gateway/ ./gateway/
COPY core/model/ ./core/model/

RUN chmod +x gradlew \
    && ./gradlew --no-daemon -p gateway clean installDist

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /src/gateway/build/install/gateway/ /app/

RUN mkdir -p /app/data

ENV HOST=0.0.0.0
ENV USAGE_LOG_PATH=/app/data/usage.csv

EXPOSE 8080

ENTRYPOINT ["/app/bin/gateway"]
