FROM eclipse-temurin:17-jre AS builder
WORKDIR /app
COPY . .
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:17-jre AS runner
WORKDIR /app
COPY --from=builder /app/server/build/libs/api-ktor-server-0.0.1.jar .
ENTRYPOINT [ "java", "-jar", "api-ktor-server-0.0.1.jar"]
