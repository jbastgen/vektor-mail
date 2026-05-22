# Multi-stage build: compile then create a lean runtime image
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build
COPY . .
RUN ./mvnw install -DskipTests --no-transfer-progress 2>/dev/null || \
    mvn install -DskipTests --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine AS runtime
LABEL maintainer="Vektor Mail Team"
LABEL description="Vektor Mail Server"

RUN addgroup -S vektor && adduser -S vektor -G vektor

WORKDIR /app

# Copy the executable JAR
COPY --from=build /build/vektor-launcher/target/vektor-launcher-*.jar app.jar

# Directories for mail data, plugins, certs
RUN mkdir -p /var/vektor/mail /var/vektor/plugins /var/vektor/index /var/vektor/backups /certs && \
    chown -R vektor:vektor /var/vektor /app

USER vektor

ENV SPRING_PROFILES_ACTIVE=prod \
    VEKTOR_PLUGINS_DIR=/var/vektor/plugins \
    VEKTOR_MAILDIR_BASE=/var/vektor/mail \
    VEKTOR_SEARCH_INDEX_DIR=/var/vektor/index

EXPOSE 25 110 143 465 587 993 995 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -q -O- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
