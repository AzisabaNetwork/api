repositories {
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    implementation(project(":common"))
    implementation("redis.clients:jedis:4.2.3")
    compileOnly("com.destroystokyo.paper:paper-api:1.15.2-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
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
        //relocate("org.jetbrains.exposed", "$prefix.org.jetbrains.exposed")
        relocate("org.jetbrains.annotations", "$prefix.org.jetbrains.annotations")
        relocate("com.zaxxer", "$prefix.com.zaxxer")
        relocate("com.charleskorn", "$prefix.com.charleskorn")
        relocate("org.snakeyaml", "$prefix.org.snakeyaml")
        relocate("redis.clients", "$prefix.redis.clients")
        relocate("org.json", "$prefix.org.json")
        relocate("org.apache.commons", "$prefix.org.apache.commons")
        relocate("com.google.gson", "$prefix.com.google.gson")
        exclude("org/intellij/lang/annotations/**")
        exclude("org/slf4j/**")
    }
}
