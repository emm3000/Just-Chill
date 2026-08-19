package com.emm.buildlogic

import com.emm.buildlogic.internal.BuildConventions
import com.emm.buildlogic.internal.libs
import com.emm.buildlogic.internal.library
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class DetektConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("dev.detekt")

        extensions.configure<DetektExtension> {
            parallel.set(true)
            buildUponDefaultConfig.set(true)
            autoCorrect.set(false)
            config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
            baseline.set(file("$rootDir/config/detekt/baseline-$name.xml"))
        }

        dependencies.apply {
            add("detektPlugins", libs.library("detekt-ktlint-wrapper"))
            add("detektPlugins", libs.library("detekt-compose-rules"))
        }

        tasks.withType<Detekt>().configureEach {
            // Both excludes are needed: Ant patterns match the path RELATIVE to each source root,
            // and SQLDelight registers `<module>/build/generated/sqldelight/code/<db>/<sourceSet>`
            // as a root, so `**/build/**` only ever sees `com/emm/data/TransactionsQueries.kt`.
            exclude("**/build/**")
            exclude { element -> BuildConventions.isGeneratedSource(element.file.invariantSeparatorsPath) }
            jvmTarget.set(BuildConventions.JVM_TARGET)
            reports {
                html.required.set(true)
                sarif.required.set(false)
                checkstyle.required.set(false)
            }
        }
    }
}
