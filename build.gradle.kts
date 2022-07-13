val kotlinVersion: String by project
val exposedVersion: String by project

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `java-library`
}

group = "net.azisaba.api"
version = "0.0.1"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    api("org.mariadb.jdbc:mariadb-java-client:3.0.6")
    api("com.zaxxer:HikariCP:5.0.1")
    api("com.charleskorn.kaml:kaml:0.46.0") // YAML support for kotlinx.serialization
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.serialization")
        plugin("com.github.johnrengelman.shadow")
        plugin("java-library")
    }

    group = parent!!.group
    version = parent!!.version

    java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

    repositories {
        mavenCentral()
    }

    tasks {
        shadowJar {
            archiveFileName.set("${parent!!.name}-${this@subprojects.name}-${project.version}.jar")
        }
    }
}
