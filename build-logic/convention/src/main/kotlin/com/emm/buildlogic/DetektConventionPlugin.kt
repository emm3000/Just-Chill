package com.emm.buildlogic

import com.emm.buildlogic.internal.library
import com.emm.buildlogic.internal.libs
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektReport
import dev.detekt.gradle.extensions.DetektReportType
import dev.detekt.gradle.extensions.FailOnSeverity
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.register

class DetektConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val cli: NamedDomainObjectProvider<Configuration> = toolClasspath(CLI_CONFIGURATION, CLI_LIBRARY)
        val rules: NamedDomainObjectProvider<Configuration> = toolClasspath(RULES_CONFIGURATION, *RULE_LIBRARIES)
        registerDetekt(cli, rules)
    }

    private fun Project.registerDetekt(
        cli: NamedDomainObjectProvider<Configuration>,
        rules: NamedDomainObjectProvider<Configuration>,
    ) {
        val root: Directory = isolated.rootProject.projectDirectory
        val corrects: Provider<Boolean> = correctsOutsideCi()
        tasks.register<Detekt>(TASK) {
            description = "Runs detekt over every Kotlin source under src/ of this module, without type resolution."
            detektClasspath.from(cli)
            pluginClasspath.from(rules)
            setSource(fileTree(SOURCE_DIRECTORY) { include(KOTLIN_SOURCES) })
            config.from(root.file(CONFIG_FILE))
            basePath.set(root.asFile.absolutePath)
            buildUponDefaultConfig.set(true)
            allRules.set(false)
            disableDefaultRuleSets.set(false)
            autoCorrect.set(corrects)
            outputs.cacheIf { !corrects.get() }
            parallel.set(true)
            debug.set(false)
            ignoreFailures.set(false)
            failOnSeverity.set(FailOnSeverity.Error)
            noJdk.set(false)
            multiPlatformEnabled.set(false)
            reports {
                listOf(checkstyle, html, markdown, sarif).forEach { report: DetektReport ->
                    report.required.set(report.type == DetektReportType.SARIF)
                    report.outputLocation.set(layout.buildDirectory.file("$REPORTS_DIRECTORY/$TASK.${report.type.extension}"))
                }
            }
        }
    }

    private fun Project.toolClasspath(name: String, vararg aliases: String): NamedDomainObjectProvider<Configuration> {
        val classpath: NamedDomainObjectProvider<Configuration> = configurations.register(name) {
            isCanBeConsumed = false
        }
        aliases.forEach { alias -> dependencies.add(name, libs.library(alias)) }
        return classpath
    }

    private fun Project.correctsOutsideCi(): Provider<Boolean> =
        providers.environmentVariable(CI_VARIABLE).map { value -> value != CI_VALUE }.orElse(true)

    companion object {
        const val TASK: String = "detekt"

        private const val CLI_CONFIGURATION: String = "detekt"
        private const val RULES_CONFIGURATION: String = "detektPlugins"
        private const val CLI_LIBRARY: String = "detekt-cli"
        private val RULE_LIBRARIES: Array<String> = arrayOf("detekt-ktlint-wrapper", "detekt-compose-rules")
        private const val SOURCE_DIRECTORY: String = "src"
        private const val KOTLIN_SOURCES: String = "**/*.kt"
        private const val CONFIG_FILE: String = "config/detekt/detekt.yml"
        private const val REPORTS_DIRECTORY: String = "reports/detekt"
        private const val CI_VARIABLE: String = "CI"
        private const val CI_VALUE: String = "true"
    }
}
