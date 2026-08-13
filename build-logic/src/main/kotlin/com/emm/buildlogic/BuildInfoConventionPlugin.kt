package com.emm.buildlogic

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/**
 * Registers [GenerateBuildInfoTask] and feeds its output into every variant's Kotlin sources.
 *
 * Applied by :androidApp alone, which is where `BuildConfig` already lives and where the platform
 * Koin module turns build-time constants into injectable values. Kept out of the module's own
 * build file for the same reason as [IosSupabaseConfigConventionPlugin]: a code generator does not
 * belong in a file whose job is declaring dependencies.
 *
 * ### Why the variant API and not `sourceSets["main"].kotlin.srcDir(task)`
 *
 * Neither half of that sentence exists here any more. AGP 9 owns Kotlin for :androidApp, and its
 * Kotlin extension holds only the eight per-variant source sets — there is no `main`
 * KotlinSourceSet to hang a srcDir on. AGP's OWN `main` source set does exist, but AGP 9 refuses a
 * `Provider` there outright: *"You cannot add Provider instances to the Android SourceSet API […]
 * Instead you should use the Sources interface in the Variant API"*, because Studio cannot tell a
 * generated read-only directory from a static read-write one. The escape hatch
 * (`android.sourceset.disallowProvider=false`) is worse than the rule — it warns that the task
 * dependency is then NOT carried, which is the entire reason for passing a provider.
 * `addGeneratedSourceDirectory` carries it, and lets AGP own the output location, so this file
 * never names a path under build/.
 *
 * The KMP modules keep `kotlin.srcDir(taskProvider)` (see [IosSupabaseConfigConventionPlugin]):
 * that is the Kotlin Multiplatform extension, which AGP's source-set rule does not govern.
 *
 * ### Why `withPlugin` and not a plain `extensions.configure`
 *
 * `ApplicationAndroidComponentsExtension` is registered by the application plugin, so configuring it
 * straight out of `apply()` reads whatever is registered at that instant — which made this plugin
 * depend on the order of the `plugins { }` block. Measured, not assumed: with the eager call, moving
 * `id("justchill.build.info")` above `alias(libs.plugins.android.application)` in
 * androidApp/build.gradle.kts fails configuration with *"Extension of type
 * 'ApplicationAndroidComponentsExtension' does not exist"*. With `withPlugin` the block runs when
 * the application plugin arrives, and both orders generate `BuildInfo.kt` — both were built.
 *
 * The AGP application marker is declared in build-logic/build.gradle.kts for the same reason. The
 * type used to arrive transitively through the `android.kotlin.multiplatform.library` marker, so
 * this build compiled against a dependency it never asked for.
 *
 * ### Why the hash is read here and not in the task
 *
 * `providers.exec { }` resolved at configuration time is a configuration-cache *input*: Gradle
 * re-runs the command when it checks whether a cached entry is still valid, reuses the entry while
 * HEAD is unchanged, and discards it the moment a new commit lands — which is exactly the
 * invalidation this feature needs, and the same mechanism `versionCode` / `versionName` already
 * ride on in androidApp/build.gradle.kts. Reading git from inside the task action instead would
 * make the task's output invisible to up-to-date checking: it would either never rerun after a
 * commit, or rerun on every build.
 */
class BuildInfoConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.withPlugin("com.android.application") {
            // Read once, not once per variant: four execs would be four chances to disagree, and
            // the hash is a property of the checkout, not of the variant.
            val hash = gitCommitHash()

            extensions.configure<ApplicationAndroidComponentsExtension> {
                onVariants { variant ->
                    // One task per variant because addGeneratedSourceDirectory assigns the output
                    // directory itself; one shared task would have four variants fighting over it.
                    val generate = tasks.register<GenerateBuildInfoTask>(
                        "generate${variant.name.replaceFirstChar(Char::uppercase)}BuildInfo",
                    ) {
                        description = "Generates BuildInfo.kt from the git commit HEAD points at."
                        commitHash.set(hash)
                    }
                    variant.sources.kotlin?.addGeneratedSourceDirectory(
                        generate,
                        GenerateBuildInfoTask::outputDirectory,
                    )
                }
            }
        }
    }

    /**
     * HEAD's full SHA, or [GenerateBuildInfoTask.UNKNOWN_COMMIT] when git cannot answer.
     *
     * Optional at configure time, deliberately, exactly like the signing credentials next door:
     * a source export or a checkout without git on PATH must still configure and build. Nothing
     * about the app depends on knowing the commit — only the footer that reports it does.
     */
    private fun Project.gitCommitHash(): String = runCatching {
        providers.exec {
            commandLine("git", "rev-parse", "HEAD")
        }.standardOutput.asText.get().trim()
    }.getOrDefault(GenerateBuildInfoTask.UNKNOWN_COMMIT)
}
