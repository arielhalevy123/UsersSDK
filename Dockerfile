# Use OpenJDK as base
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy build output
COPY build/libs/*.jar app.jar

# Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]