package com.emm.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Registers [GenerateIosSupabaseConfigTask] and feeds its output into iosMain.
 *
 * Applied by :presentation alone (home of KoinIos.kt, the config's only consumer — :ui-android
 * until slice S2). That is deliberate: the point is separation, not reuse — a code generator does
 * not belong in a file whose job is declaring dependencies.
 */
class IosSupabaseConfigConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val generate = tasks.register<GenerateIosSupabaseConfigTask>("generateIosSupabaseConfig") {
            description = "Generates IosSupabaseConfig.kt from root supabase.properties."
            // Set only when the file is really there. `@Optional` on the task's `@InputFile` means
            // the property may be UNSET; it does not mean the path may point at nothing. Once a
            // path is assigned, Gradle validates its existence and fails with "An input file was
            // expected to be present but it doesn't exist" before the task ever runs — so the
            // fallback GenerateIosSupabaseConfigTask already implements was unreachable.
            //
            // The file is absent on a fresh clone and on every Dependabot PR (those runs get no
            // repo secrets at all, so the setup action writes nothing). This failure sat hidden
            // behind the keystore crash in androidApp/build.gradle.kts and only surfaced once that
            // one was fixed. It reaches the Linux gate too, not just the iOS compile: the task's
            // output is an iosMain srcDir, so detektIosMainSourceSet realises it.
            val supabaseProperties = rootProject.layout.projectDirectory.file("supabase.properties")
            if (supabaseProperties.asFile.exists()) {
                propertiesFile.set(supabaseProperties)
            }
            outputDirectory.set(layout.buildDirectory.dir("generated/iosSupabaseConfig/kotlin"))
        }

        extensions.configure<KotlinMultiplatformExtension> {
            // The TaskProvider is passed to srcDir on purpose, not the bare directory: srcDir then
            // carries the task dependency through the source set's declared outputs, so EVERY
            // consumer of iosMain — Kotlin/Native compiles, detekt, detekt baseline, IDE sync —
            // depends on the generator implicitly. Registering the plain directory instead makes
            // Gradle 9 fail with "uses this output of task ':ui-android:generateIosSupabaseConfig'
            // without declaring an explicit or implicit dependency" for any consumer lacking its own
            // dependsOn (detektIosMainSourceSet hit exactly that once it joined the KMP gate).
            //
            // Registered lazily because `iosMain` is an intermediate source set created by the KMP
            // default hierarchy template, not by the iosArm64()/iosSimulatorArm64() calls — an eager
            // getByName("iosMain") fails with "KotlinSourceSet with name 'iosMain' not found".
            sourceSets.matching { it.name == "iosMain" }.configureEach {
                kotlin.srcDir(generate)
            }
        }
    }
}
