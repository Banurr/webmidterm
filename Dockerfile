FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle

RUN ./gradlew --version

COPY . .

RUN ./gradlew --no-daemon bootJar \
    && cp "$(find build/libs -name '*.jar' ! -name '*-plain.jar')" app.jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]