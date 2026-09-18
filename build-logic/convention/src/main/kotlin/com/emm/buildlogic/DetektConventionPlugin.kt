package com.emm.buildlogic

import com.emm.buildlogic.internal.BuildConventions
import com.emm.buildlogic.internal.library
import com.emm.buildlogic.internal.libs
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceTask
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

class DetektConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("dev.detekt")

        val baselineName: String = BuildConventions.baselineNameOf(path)

        extensions.configure<DetektExtension> {
            parallel.set(true)
            buildUponDefaultConfig.set(true)
            autoCorrect.set(false)
            config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
            baseline.set(file("$rootDir/config/detekt/baseline-$baselineName.xml"))
        }

        dependencies.apply {
            add("detektPlugins", libs.library("detekt-ktlint-wrapper"))
            add("detektPlugins", libs.library("detekt-compose-rules"))
        }

        tasks.withType<Detekt>().configureEach {
            excludeGeneratedSources()
            jvmTarget.set(BuildConventions.JVM_TARGET)
            reports {
                html.required.set(true)
                sarif.required.set(false)
                checkstyle.required.set(false)
            }
        }

        tasks.withType<DetektCreateBaselineTask>().configureEach {
            excludeGeneratedSources()
        }

        putOwnClassesOnAnalysisClasspath()
    }

    // Both excludes are needed: Ant patterns match the path RELATIVE to each source root, and
    // SQLDelight registers `<module>/build/generated/sqldelight/code/<db>/<sourceSet>` as a root,
    // so `**/build/**` only ever sees `com/emm/justchill/core/database/TransactionsQueries.kt`.
    private fun SourceTask.excludeGeneratedSources() {
        exclude("**/build/**")
        exclude { element -> BuildConventions.isGeneratedSource(element.file.invariantSeparatorsPath) }
    }

    // AGP 9's built-in Kotlin leaves `compilation.output.classesDirs` EMPTY and `BuildConfig` compiles
    // to the javac output, so detekt's own `classpath` convention reaches neither half of a module's
    // own classes: it answers the unresolved symbols by downgrading the file to untyped analysis.
    private fun Project.putOwnClassesOnAnalysisClasspath() {
        val kotlinTarget = extensions.findByType(KotlinAndroidExtension::class.java)?.target ?: return
        kotlinTarget.compilations.configureEach {
            val androidCompilation: KotlinJvmAndroidCompilation =
                this as? KotlinJvmAndroidCompilation ?: return@configureEach
            val compileTask = compileTaskProvider.map { it as KotlinJvmCompile }
            val classes = compileTask.flatMap { it.destinationDirectory }
            val libraries = compileTask.map { it.libraries }
            val friends = compileTask.map { it.friendPaths }
            val javaClasses: Provider<Directory> = androidCompilation.compileJavaTaskProvider
                .flatMap { it.destinationDirectory }
            val suffix = name.replaceFirstChar(Char::uppercase)

            tasks.withType<Detekt>().matching { it.name == "detekt$suffix" }.configureEach {
                classpath.setFrom(classes, javaClasses, libraries)
                friendPaths.setFrom(classes, friends)
            }
            tasks.withType<DetektCreateBaselineTask>()
                .matching { it.name == "detektBaseline$suffix" }
                .configureEach {
                    classpath.setFrom(classes, javaClasses, libraries)
                    friendPaths.setFrom(classes, friends)
                }
        }
    }
}
