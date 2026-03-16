FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

