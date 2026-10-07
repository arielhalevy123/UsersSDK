# Multi-stage build: compiles the jar inside Docker, so the image builds from a clean checkout
# (Railway, CI) without a prebuilt build/libs/*.jar.

# ---- build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /workspace/build/libs/app.jar app.jar
USER appuser

# The app listens on $PORT (Railway sets it); 8080 when unset. EXPOSE is documentation only.
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
