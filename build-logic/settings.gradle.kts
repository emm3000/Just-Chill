// Included build (composite) that hosts this project's convention plugins. It is wired in from the
// root settings.gradle.kts via pluginManagement { includeBuild("build-logic") }, which is why no
// module ever declares a version for `justchill.*` plugin ids.
//
// It re-reads the SAME gradle/libs.versions.toml the main build uses, so plugin versions stay
// declared in exactly one file.

dependencyResolutionManagement {
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
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
