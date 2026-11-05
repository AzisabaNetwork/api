repositories {
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    implementation(project(":common"))
    compileOnly("com.velocitypowered:velocity-api:3.0.1")
}

tasks {
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
        relocate("it.krzeminski.snakeyaml", "$prefix.it.krzeminski.snakeyaml")
        relocate("okio", "$prefix.okio")
        relocate("net.thauvin.erik", "$prefix.net.thauvin.erik")
        exclude("org/intellij/lang/annotations/**")
        exclude("org/slf4j/**")
    }
}
