plugins {
    id("com.jetbrains.exposed.gradle.plugin") version "0.2.1"
}

repositories {
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    implementation(project(":"))
    compileOnly("com.velocitypowered:velocity-api:3.0.1")
}

tasks {
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    shadowJar {
        mergeServiceFiles()
        val prefix = "net.azisaba.api.velocity.lib"
        relocate("org.mariadb", "$prefix.org.mariadb")
        relocate("kotlinx", "$prefix.kotlinx")
        relocate("kotlin", "$prefix.kotlin")
        //relocate("org.jetbrains.exposed", "$prefix.org.jetbrains.exposed")
        relocate("org.jetbrains.annotations", "$prefix.org.jetbrains.annotations")
        relocate("com.zaxxer", "$prefix.com.zaxxer")
        relocate("com.charleskorn", "$prefix.com.charleskorn")
        relocate("org.snakeyaml", "$prefix.org.snakeyaml")
        exclude("org/intellij/lang/annotations/**")
        exclude("org/slf4j/**")
    }
}
