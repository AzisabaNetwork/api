rootProject.name = "api-ktor"
include("server")
include("velocity")
include("spigot")
include("common")
include("spigot-mythic4_12")
include("spigot-common")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}
