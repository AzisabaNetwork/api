val ktorVersion = providers.gradleProperty("ktorVersion").get()
val logbackVersion = providers.gradleProperty("logbackVersion").get()
val exposedVersion = providers.gradleProperty("exposedVersion").get()
val adventureVersion = "4.26.1"

plugins {
    application
}

application {
    mainClass.set("net.azisaba.api.server.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(project(":common"))
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-resources:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("redis.clients:jedis:8.0.2")
    implementation("net.azisaba.interchat:api:2.12.0")
    implementation("net.kyori:adventure-api:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-gson:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-plain:$adventureVersion")
    implementation("org.cloudburstmc:nbt:3.1.0.Final")
    implementation("net.luckperms:api:5.5")
    implementation("cloud.unum:usearch:2.8.12")
    implementation("com.github.jelmerk:hnswlib-core:1.2.1")
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation(kotlin("test-junit"))
}
