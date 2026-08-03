FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon && \
    cp "$(find build/libs -name '*.jar' ! -name '*plain.jar' | head -n 1)" app.jar

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/app.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
