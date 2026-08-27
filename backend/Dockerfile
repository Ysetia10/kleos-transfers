# Production image for Render (or any container host).
# Build context: repository root (see render.yaml dockerContext).

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY backend/gradle backend/gradle
COPY backend/gradlew backend/build.gradle backend/settings.gradle backend/
COPY backend/src backend/src
WORKDIR /workspace/backend
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /workspace/backend/build/libs/*.jar /app/app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:TieredStopAtLevel=1", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/app.jar"]
