# syntax=docker/dockerfile:1.7
# Base images are pinned by digest (multi-architecture index), so a rebuild uses the same bytes until
# the pin is refreshed on purpose. Refresh (monthly, and for every base image security fix):
#   docker buildx imagetools inspect maven:3.9-eclipse-temurin-21  | grep -m1 Digest
#   docker buildx imagetools inspect eclipse-temurin:21-jre-alpine | grep -m1 Digest
# then replace the sha256 values below, rebuild, and let the pipeline's image scan pass.
# ---- build stage -------------------------------------------------------------------
FROM maven:3-eclipse-temurin-26@sha256:b2c1ad85954592f9928e84327c65201f308ad9b5d8ed7d5b823717c97bf23fbb AS build
WORKDIR /workspace
COPY pom.xml .
COPY quality quality
RUN mvn -B -q dependency:go-offline
COPY src src
# Quality gates run in CI; the image build only packages the tested code.
RUN mvn -B -q -DskipTests -Djacoco.skip=true -Dcheckstyle.skip -Dpmd.skip -Dcpd.skip \
    -Dspotbugs.skip -Dspotless.check.skip=true -Dcyclonedx.skip=true package

# ---- runtime stage -----------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine@sha256:51ab5e3302e7141ce665ca3ea85e8b5cd648eafbc3c0c90dd79d6537684e4555
# Security fixes of the Alpine packages published after the pinned base image; fixed uid/gid 10001,
# the runAsUser / runAsGroup of the Kubernetes deployments.
RUN apk upgrade --no-cache && addgroup -S -g 10001 brokerverse && adduser -S -u 10001 -G brokerverse -H -s /sbin/nologin brokerverse
WORKDIR /app
COPY --from=build /workspace/target/brokerverse-backend.jar app.jar
USER 10001:10001
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
