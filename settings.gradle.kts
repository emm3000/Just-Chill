@file:Suppress("UnstableApiUsage")

// Gradle's canonical settings order: pluginManagement -> plugins -> dependencyResolutionManagement
// -> project name -> includes. Keep it that way; `include(":core:database")` used to sit above
// pluginManagement, which read as if :core:database were special. It is not.

pluginManagement {
    // Hosts the `justchill.*` convention plugins. Because it is an included build, modules apply
    // them by id with no version — see build-logic/convention/.
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

// Lets modules reference each other as `projects.core.ui` instead of `project(":core:ui")`.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "JustChill"

// The module map. Read the middle column as the question each module answers.
//
//   :core:domain    who am I?         business rules, zero framework   JVM only
//   :core:database  where do I live?  SQLDelight                       Android
//   :core:backup    who keeps a copy? Supabase, Ktor, the Snapshot     Android
//   :core:ui        how do I show it? MVI, formatters, design system   Android
//   :core:testing   how do I test?    MainDispatcherRule, FakeTodayFlow JVM only
//   :feature:*      what do I let you do? one screen family each      Android
//   :androidApp     how do I ship?    manifest, shell, Koin graph      Android only
//
// Dependency order, top of the graph down:
//   androidApp -> feature:*, core:backup, core:database, core:ui -> core:domain, and a feature
//   reaches core:ui and core:domain only, plus core:testing as a test fixture.
include(":androidApp")
include(":core:backup")
include(":core:database")
include(":core:domain")
include(":core:testing")
include(":core:ui")
include(":feature:transaction")
include(":feature:account")
include(":feature:category")
include(":feature:recurring")
include(":feature:report")
include(":feature:loan")
include(":feature:profile")
include(":feature:auth")
include(":feature:onboarding")
