// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.google.crashlytics) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.detekt)
}

val detektKtlintWrapper = libs.detekt.ktlint.wrapper
val detektComposeRules = libs.detekt.compose.rules

subprojects {
    apply(plugin = "dev.detekt")

    detekt {
        parallel = true
        buildUponDefaultConfig = true
        autoCorrect = false
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        baseline = file("$rootDir/config/detekt/baseline.xml")
    }

    dependencies {
        "detektPlugins"(detektKtlintWrapper)
        "detektPlugins"(detektComposeRules)
    }

    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        jvmTarget.set("17")
        reports {
            html.required.set(true)
            sarif.required.set(false)
            checkstyle.required.set(false)
        }
    }
}