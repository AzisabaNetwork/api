rootProject.name = "api-ktor"
include("server")
include("velocity")
include("spigot")
include("common")
include("spigot-mythic4_12")
include("spigot-mythic5_12")
include("spigot-common")
include("spigot-folia")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
