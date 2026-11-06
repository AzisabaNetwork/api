import org.jetbrains.kotlin.gradle.dsl.JvmTarget

repositories {
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    api(project(":common"))
    api(project(":spigot-common"))
    api(project(":spigot-mythic4_12"))
    api(project(":spigot-mythic5_12"))
    api(project(":spigot-folia"))
    implementation("redis.clients:jedis:8.0.2")
    // JUnit 6 requires Java 17, but this module tests on the Java 8 toolchain.
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        mergeServiceFiles()
        val prefix = "net.azisaba.api.spigot.lib"
        relocate("org.mariadb", "$prefix.org.mariadb")
        relocate("kotlinx", "$prefix.kotlinx")
        relocate("kotlin", "$prefix.kotlin")
        // relocate("org.jetbrains.exposed", "$prefix.org.jetbrains.exposed")
        relocate("org.jetbrains.annotations", "$prefix.org.jetbrains.annotations")
        relocate("com.zaxxer", "$prefix.com.zaxxer")
        relocate("com.charleskorn", "$prefix.com.charleskorn")
        relocate("it.krzeminski.snakeyaml", "$prefix.it.krzeminski.snakeyaml")
        relocate("okio", "$prefix.okio")
        relocate("net.thauvin.erik", "$prefix.net.thauvin.erik")
        relocate("com.google.errorprone", "$prefix.com.google.errorprone")
        relocate("redis.clients", "$prefix.redis.clients")
        relocate("org.json", "$prefix.org.json")
        relocate("org.apache.commons", "$prefix.org.apache.commons")
        relocate("com.google.gson", "$prefix.com.google.gson")
        exclude("org/intellij/lang/annotations/**")
        exclude("org/slf4j/**")
    }
}
