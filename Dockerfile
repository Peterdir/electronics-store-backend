FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn ./.mvn
COPY src ./src

RUN chmod +x mvnw && ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar
RUN mkdir -p /app/uploads

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
