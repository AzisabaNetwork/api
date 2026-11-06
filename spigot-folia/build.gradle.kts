repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))
java.disableAutoTargetJvm()

dependencies {
    compileOnly(project(":spigot-common")) {
        exclude(group = "com.destroystokyo.paper")
    }
    compileOnly("dev.folia:folia-api:26.3.build.5-alpha") {
        exclude(group = "org.joml")
    }
}
