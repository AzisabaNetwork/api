FROM eclipse-temurin:17-jre AS builder
WORKDIR /app
COPY . .
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:17-jre AS runner
WORKDIR /app
# Copy without the version in the name so a version bump does not break the image build.
COPY --from=builder /app/server/build/libs/api-ktor-server-*.jar ./api-ktor-server.jar
ENTRYPOINT [ "java", "-jar", "api-ktor-server.jar"]
