plugins {
    kotlin("jvm") version "2.4.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
    `java-library`
}

repositories {
    mavenCentral()
}

group = "net.azisaba.api"
version = "0.0.3"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.serialization")
        plugin("com.gradleup.shadow")
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
            // Shadow 9 defaults to EXCLUDE, which drops duplicates before transformers such as
            // mergeServiceFiles() can process them. Keep the Shadow 8 behavior.
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            archiveFileName.set("${parent!!.name}-${this@subprojects.name}-${project.version}.jar")
        }
    }
}
