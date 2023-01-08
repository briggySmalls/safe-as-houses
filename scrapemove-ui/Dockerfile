# first stage
FROM sbtscala/scala-sbt:eclipse-temurin-17.0.5_8_1.8.0_2.13.10 AS build
COPY ./ ./
RUN sbt clean assembly

# Second stage
FROM eclipse-temurin:17-jdk
COPY --from=build /root/target/scala-2.13/*.jar /app.jar
CMD ["java", "-jar", "app.jar"]