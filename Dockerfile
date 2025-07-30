FROM openjdk:26-oraclelinux9
WORKDIR /app
COPY target/TelegramBot-1.0-SNAPSHOT.jar .
CMD ["java", "-jar", "TelegramBot-1.0-SNAPSHOT.jar"]