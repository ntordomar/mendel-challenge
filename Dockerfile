FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml
COPY src src
RUN chmod +x mvnw && ./mvnw -B -DskipTests package \
    && cp target/transactions-*.jar /app/application.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/application.jar /app/application.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
