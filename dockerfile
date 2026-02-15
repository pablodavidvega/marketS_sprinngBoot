FROM maven:3.9.6-eclipse-temurin-21
WORKDIR /app

COPY . .

RUN mvn dependency:resolve

RUN mvn clean package -DskipTests --fail-never -Dmaven.retry.failedDownloadCount=3

RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/MarketS-0.0.1-SNAPSHOT.jar"]
# Forzar rebuild completo Feb 15
