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

/**
 * detekt setup for every module.
 *
 * This used to live in the root build.gradle.kts inside a `subprojects { }` block. That worked, but
 * cross-project configuration is what blocks Gradle's Project Isolation, and it configured modules
 * from the outside rather than letting each one opt in. Each module now applies `justchill.detekt`.
 *
 * Note for the gate: plain `./gradlew detekt` is NO-SOURCE on the KMP modules. Real coverage comes
 * from the per-source-set tasks (detektMainAndroid, detektIosMainSourceSet, ...) — the per-task
 * baseline mapping is in docs/archive/kmp/ORCHESTRATION.md; the gate itself is docs/WORKFLOW.md
 * and the pre-push hook.
 */
class DetektConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("dev.detekt")

        // `.set(...)` rather than `=`. The assignment sugar for Gradle's Property<T> comes from a
        // compiler plugin that Gradle enables for .gradle.kts scripts only; in a plain Kotlin class
        // `parallel = true` fails to compile with "Unresolved reference 'assign'".
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
            // Generated sources are not ours to style. Compose's resource generator and the iOS
            // Supabase config generator both write into build/, and linting them produced 13
            // findings about indentation in a file no human will ever edit.
            //
            // Both excludes are needed. Ant patterns are matched against the path RELATIVE to each
            // source root, so `**/build/**` never fires for a root that is itself inside build/ —
            // SQLDelight registers `<module>/build/generated/sqldelight/code/<db>/<sourceSet>` as a
            // root, leaving relative paths like `com/emm/data/TransactionsQueries.kt`. The spec below
            // sees the absolute path and catches those.
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
