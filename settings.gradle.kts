pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://jitpack.io")
    }

    resolutionStrategy {
        eachPlugin {
            // if (requested.id.id.startsWith("com.ageet.git-repo")) {
            //     useModule("com.github.atsushi-ageet:gradle-git-repo-plugin:${requested.version}")
            // }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    // id("com.ageet.git-repo") version("3.0.2")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
    versionCatalogs {
        create("ktorLibs").from("io.ktor:ktor-version-catalog:3.6.0")
    }
}

rootProject.name = "ktor-serverless-sample"
