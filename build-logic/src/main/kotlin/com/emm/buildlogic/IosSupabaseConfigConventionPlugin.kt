package com.emm.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class IosSupabaseConfigConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val generate = tasks.register<GenerateIosSupabaseConfigTask>("generateIosSupabaseConfig") {
            description = "Generates IosSupabaseConfig.kt from root supabase.properties."
            // `@Optional` on the task's `@InputFile` means the property may be UNSET, not that an
            // assigned path may point at nothing — Gradle validates it and fails before the task
            // runs. The file is absent on a fresh clone and on every Dependabot PR.
            val supabaseProperties = rootProject.layout.projectDirectory.file("supabase.properties")
            if (supabaseProperties.asFile.exists()) {
                propertiesFile.set(supabaseProperties)
            }
            outputDirectory.set(layout.buildDirectory.dir("generated/iosSupabaseConfig/kotlin"))
        }

        extensions.configure<KotlinMultiplatformExtension> {
            // Matched rather than fetched: `iosMain` is created by the KMP default hierarchy
            // template, not by the iosArm64()/iosSimulatorArm64() calls, so it does not exist yet.
            sourceSets.matching { it.name == "iosMain" }.configureEach {
                // The TaskProvider, not the bare directory: srcDir then carries the task
                // dependency, so every consumer of iosMain — Kotlin/Native compiles, detekt,
                // detekt baseline, IDE sync — depends on the generator without its own dependsOn.
                kotlin.srcDir(generate)
            }
        }
    }
}
