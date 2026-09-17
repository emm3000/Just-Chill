package com.emm.buildlogic

import com.emm.buildlogic.internal.ModuleRole
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CheckModuleBoundariesTask : DefaultTask() {

    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val dependencyPaths: SetProperty<String>

    @get:Input
    abstract val android: Property<Boolean>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val path: String = modulePath.get()
        val role: ModuleRole = ModuleRole.of(path)
        val forbidden: List<String> = dependencyPaths.get().sorted().filterNot(role::allows)
        val violations: List<String> = buildList {
            forbidden.forEach { add("$path depends on $it") }
            if (role == ModuleRole.CORE_DOMAIN && android.get()) {
                add("$path applies an Android plugin")
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                violations.joinToString(
                    separator = "\n",
                    prefix = "Module boundary violated: ${role.rule()}.\n",
                ),
            )
        }

        val reportFile: File = report.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(dependencyPaths.get().sorted().joinToString(separator = "\n", postfix = "\n"))
    }
}
