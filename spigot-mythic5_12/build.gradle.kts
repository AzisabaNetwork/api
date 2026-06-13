repositories {
    maven { url = uri("https://mvn.lumine.io/repository/maven-public/") }
    maven("https://repo.papermc.io/repository/maven-public/")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

dependencies {
    compileOnlyApi(project(":spigot-common"))
    compileOnly("io.lumine:Mythic:5.12.0-SNAPSHOT")
    compileOnly("io.lumine:LumineUtils:1.21-SNAPSHOT")
}
