FROM maven:3.9.6-eclipse-temurin-21

WORKDIR /app

COPY . .

RUN mvn dependency:resolve
RUN mvn clean package -DskipTests -Dmaven.retry.failedDownloadCount=3

EXPOSE 8080

CMD ["java", "-jar", "target/MarketS-0.0.1-SNAPSHOT.jar"]
