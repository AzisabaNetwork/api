val kotlinVersion: String by project

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    api("org.mariadb.jdbc:mariadb-java-client:3.0.6")
    api("com.zaxxer:HikariCP:5.0.1")
    api("com.charleskorn.kaml:kaml:0.55.0") // YAML support for kotlinx.serialization
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

tasks {
    compileJava {
        targetCompatibility = "1.8"
        sourceCompatibility = "1.8"
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}
