# Multi-stage build for Spring Boot application

# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
# If you want to run tests during build, remove -DskipTests
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Install curl for healthchecks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to spring user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose port (Railway will set PORT env variable)
EXPOSE 8080

# Set production profile
ENV SPRING_PROFILES_ACTIVE=prod

# JVM options for container environment
# Using G1GC for better performance and container-aware memory settings
ENV JAVA_OPTS="-Xms256m -Xmx700m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC"

# Health check
# Increased start-period to 90s for slow startup (StockWarmer @PostConstruct loads Redis)
# Increased interval to 60s to reduce overhead in production
HEALTHCHECK --interval=60s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# Run the application
# Use exec form for proper signal handling (SIGTERM for graceful shutdown)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
