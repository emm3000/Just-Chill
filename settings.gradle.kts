@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "JustChill"

include(":androidApp")
include(":core:backup")
include(":core:database")
include(":core:domain")
include(":core:testing")
include(":core:ui")
include(":feature:transaction")
include(":feature:account")
include(":feature:category")
include(":feature:report")
include(":feature:loan")
include(":feature:profile")
include(":feature:auth")
include(":feature:onboarding")
