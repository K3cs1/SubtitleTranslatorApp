FROM eclipse-temurin:25-jre

WORKDIR /app
COPY app.jar app.jar

# Elastic Beanstalk Docker platform typically proxies to container port 5000 by default.
# We bind Spring Boot to this port inside the container via application.yml (server.port=${PORT:8080}).
ENV PORT=5000

EXPOSE 5000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
