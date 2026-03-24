FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY ../pom.xml ../pom.xml
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    cd .. && mvn -pl config-server -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
