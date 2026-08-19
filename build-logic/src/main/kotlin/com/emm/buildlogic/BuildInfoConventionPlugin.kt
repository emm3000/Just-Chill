package com.emm.buildlogic

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

class BuildInfoConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.withPlugin(APPLICATION_PLUGIN) {
            val hash = gitCommitHash()

            extensions.configure<ApplicationAndroidComponentsExtension> {
                onVariants { variant ->
                    // One task per variant: addGeneratedSourceDirectory assigns the output
                    // directory itself, so a shared task would have four variants fighting over it.
                    val generate = tasks.register<GenerateBuildInfoTask>(
                        "generate${variant.name.replaceFirstChar(Char::uppercase)}BuildInfo",
                    ) {
                        description = "Generates BuildInfo.kt from the git commit HEAD points at."
                        commitHash.set(hash)
                    }
                    val kotlinSources = checkNotNull(variant.sources.kotlin) {
                        "Variant '${variant.name}' of $path has no Kotlin source container, so " +
                            "the generated BuildInfo.kt would never be compiled. The " +
                            "justchill.build.info plugin needs the Kotlin plugin on this module."
                    }
                    // Not srcDir: AGP 9 refuses a task provider on its own `main` source set, and
                    // the escape hatch it names, `android.sourceset.disallowProvider=false`, drops
                    // the task dependency — a green build with nothing ordering the generator.
                    kotlinSources.addGeneratedSourceDirectory(
                        generate,
                        GenerateBuildInfoTask::outputDirectory,
                    )
                }
            }
        }

        afterEvaluate {
            check(pluginManager.hasPlugin(APPLICATION_PLUGIN)) {
                "The justchill.build.info plugin generates BuildInfo.kt through the Android " +
                    "APPLICATION variant API, but $path never applied '$APPLICATION_PLUGIN', so " +
                    "nothing was registered and no BuildInfo.kt exists. Apply it to an Android " +
                    "application module or remove it from this build file."
            }
        }
    }

    /**
     * Read at configuration time on purpose: `providers.exec` resolved here is a
     * configuration-cache input, so a new commit invalidates the cached entry. Reading git from
     * inside the task action instead would hide the change from up-to-date checking.
     */
    private fun Project.gitCommitHash(): String = runCatching {
        providers.exec {
            commandLine("git", "rev-parse", "HEAD")
        }.standardOutput.asText.get().trim()
    }.getOrDefault(GenerateBuildInfoTask.UNKNOWN_COMMIT)

    private companion object {
        const val APPLICATION_PLUGIN = "com.android.application"
    }
}
