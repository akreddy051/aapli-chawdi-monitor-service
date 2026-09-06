FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
# The context-load test requires a configured database; run it outside image builds.
RUN mvn -B -DskipTests package

# Keep this version aligned with the Playwright dependency in pom.xml.
FROM mcr.microsoft.com/playwright/java:v1.59.0-noble
WORKDIR /app
ENV PLAYWRIGHT_HEADLESS=true \
    PLAYWRIGHT_SLOWMO=0 \
    JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Kolkata -XX:MaxRAMPercentage=40.0" \
    SPRING_JPA_SHOW_SQL=false \
    SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5
COPY --from=build /build/target/aapliChawdi-0.0.1-SNAPSHOT.jar /app/app.jar
RUN mkdir -p /app/screenshots && chown -R pwuser:pwuser /app
USER pwuser
EXPOSE 9999
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
