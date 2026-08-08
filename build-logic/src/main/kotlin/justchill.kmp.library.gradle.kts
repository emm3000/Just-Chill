import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Convention plugin for the three KMP library modules: :domain, :data, :shared-ui.
//
// It owns the answer to "what am I" — targets, SDK level, JVM target, host-test source set — so
// that answer lives in ONE file instead of being copy-pasted into three build.gradle.kts.
// Each module still owns "what do I need" (its dependencies) and "what do I generate" (custom
// tasks), which is the part that genuinely differs.
//
// Deliberately NOT set here, because the modules disagree and the difference is meaningful:
//   - namespace  — one per module
//   - minSdk     — 26 for :domain / :data, 28 for :shared-ui
// Each module declares both in its own `kotlin { androidLibrary { } }` block.

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    androidLibrary {
        // Single source of truth for the project's compile SDK. Was duplicated across the three
        // module files, so bumping it meant remembering all three.
        compileSdk = 37

        // JVM-host unit tests (androidHostTest). Every KMP module here has them; :data adds an
        // instrumented device-test source set on top, in its own build file.
        withHostTest { }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    iosArm64()
    iosSimulatorArm64()
}
