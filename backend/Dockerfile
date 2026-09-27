# syntax=docker/dockerfile:1.7
# ---- build stage -------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY quality quality
RUN mvn -B -q dependency:go-offline
COPY src src
# Quality gates run in CI; the image build only packages the tested code.
RUN mvn -B -q -DskipTests -Djacoco.skip=true -Dcheckstyle.skip -Dpmd.skip -Dcpd.skip \
    -Dspotbugs.skip -Dspotless.check.skip=true package

# ---- runtime stage -----------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S brokerverse && adduser -S brokerverse -G brokerverse
WORKDIR /app
COPY --from=build /workspace/target/brokerverse-backend.jar app.jar
USER brokerverse
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
