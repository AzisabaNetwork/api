val kotlinVersion: String by project
val exposedVersion: String by project

plugins {
    kotlin("jvm") version "1.9.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `java-library`
}

repositories {
    mavenCentral()
}

group = "net.azisaba.api"
version = "0.0.1"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

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
        maven { url = uri("https://repo.azisaba.net/repository/maven-public/") }
    }

    tasks {
        processResources {
            filteringCharset = "UTF-8"
            from(sourceSets.main.get().resources.srcDirs) {
                include("**")

                val tokenReplacementMap = mapOf(
                    "version" to project.version,
                    "name" to project.rootProject.name,
                )

                filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to tokenReplacementMap)
            }

            duplicatesStrategy = DuplicatesStrategy.INCLUDE

            from(projectDir) { include("LICENSE") }
        }

        shadowJar {
            archiveFileName.set("${parent!!.name}-${this@subprojects.name}-${project.version}.jar")
        }
    }
}
