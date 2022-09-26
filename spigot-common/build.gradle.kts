repositories {
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

dependencies {
    compileOnlyApi(project(":common"))
    compileOnlyApi("com.destroystokyo.paper:paper-api:1.15.2-R0.1-SNAPSHOT")
}
