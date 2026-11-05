import org.jetbrains.kotlin.gradle.dsl.JvmTarget


dependencies {
    api("org.mariadb.jdbc:mariadb-java-client:3.5.11")
    api("com.zaxxer:HikariCP:7.1.0")
    // YAML support for kotlinx.serialization. 0.80.0+ targets Java 11, but spigot tests run on the Java 8 toolchain.
    api("com.charleskorn.kaml:kaml:0.79.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    testImplementation(kotlin("test-junit"))
}

tasks {
    compileJava {
        targetCompatibility = "1.8"
        sourceCompatibility = "1.8"
    }

    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
}
