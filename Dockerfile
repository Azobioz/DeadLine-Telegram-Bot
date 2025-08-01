FROM openjdk:26-oraclelinux9
WORKDIR /app
COPY  target/*.jar app.jar
CMD ["java", "-jar", "app.jar"]