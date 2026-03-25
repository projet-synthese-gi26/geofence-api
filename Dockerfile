FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY lib ./lib
COPY src ./src

RUN mvn -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV SPRING_PROFILES_ACTIVE=docker

RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=builder /app/target/*.jar app.jar

RUN mkdir -p /app/uploads && chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
