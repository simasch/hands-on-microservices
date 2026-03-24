FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/*.jar app.jar
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
