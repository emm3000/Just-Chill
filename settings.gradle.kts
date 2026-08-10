@file:Suppress("UnstableApiUsage")

// Gradle's canonical settings order: pluginManagement -> plugins -> dependencyResolutionManagement
// -> project name -> includes. Keep it that way; `include(":data")` used to sit above
// pluginManagement, which read as if :data were special. It is not.

pluginManagement {
    // Hosts the `justchill.*` convention plugins. Because it is an included build, modules apply
    // them by id with no version — see build-logic/src/main/kotlin/.
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

// Lets modules reference each other as `projects.uiAndroid` instead of `project(":ui-android")`.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "JustChill"

// The module map. Read the middle column as the question each module answers.
//
//   :domain        who am I?         business rules, zero framework   Android + iOS
//   :data          where do I live?  SQLDelight + Supabase            Android + iOS
//   :presentation  what do I think?  ViewModels, MVI, Koin, format    Android + iOS
//   :ui-android    how do I look?    Compose screens, nav, theme      Android only
//   :androidApp    how do I ship?    manifest, signing, flavors       Android only
//
// The line between :presentation and :ui-android is a PLATFORM boundary, not a layer boundary:
// iOS has no counterpart to the bottom two rows. iosApp/ is a native SwiftUI app sitting on the
// JustChillKit framework that :presentation exports (docs/adr/005). That is why :ui-android
// carries its platform in its name. Its old name said "shared", which read as the exact
// opposite of the truth: Compose is the one thing iOS never touches.
//
// Dependency order, top of the graph down:
//   androidApp -> ui-android -> presentation -> data -> domain
include(":androidApp")
include(":ui-android")
include(":presentation")
include(":data")
include(":domain")
