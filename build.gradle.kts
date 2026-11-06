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
            // Drop duplicate entries (e.g. every library's META-INF/LICENSE.txt): Paper's plugin
            // remapper refuses jars containing duplicate entries. Service files must stay INCLUDE
            // so mergeServiceFiles() can see and merge every copy.
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            filesMatching("META-INF/services/**") {
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
            archiveFileName.set("${parent!!.name}-${this@subprojects.name}-${project.version}.jar")
        }
    }
}
