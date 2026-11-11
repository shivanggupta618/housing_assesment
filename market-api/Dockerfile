FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY market-api/pom.xml ./pom.xml
COPY market-api/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/target/housing-market-api-1.0.0.jar ./app.jar
COPY training_data.csv /data/training_data.csv
ENV HOUSING_DATA_FILE=/data/training_data.csv
EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=6 \
    CMD curl -fsS http://127.0.0.1:8081/api/market/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
