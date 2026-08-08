@file:Suppress("UnstableApiUsage")

// Gradle's canonical settings order: pluginManagement -> plugins -> dependencyResolutionManagement
// -> project name -> includes. Keep it that way; `include(":data")` used to sit above
// pluginManagement, which read as if :data were special. It is not.

pluginManagement {
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

// Lets modules reference each other as `projects.sharedUi` instead of `project(":shared-ui")`.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "JustChill"

// Dependency order, top of the graph down: androidApp -> shared-ui -> data -> domain.
include(":androidApp")
include(":shared-ui")
include(":data")
include(":domain")
