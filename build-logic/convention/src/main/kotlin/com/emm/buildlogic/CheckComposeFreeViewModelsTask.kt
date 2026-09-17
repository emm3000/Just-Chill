package com.emm.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CheckComposeFreeViewModelsTask : DefaultTask() {

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val offenders: List<String> = sources.files
            .filter { it.isFile && importsCompose(it) }
            .map { it.invariantSeparatorsPath }
            .sorted()

        if (offenders.isNotEmpty()) {
            throw GradleException(
                offenders.joinToString(
                    separator = "\n",
                    prefix = "A ViewModel or UiState source may not import $COMPOSE_PACKAGE:\n",
                ),
            )
        }

        val reportFile: File = report.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(sources.files.size.toString())
    }

    private fun importsCompose(file: File): Boolean = file.useLines { lines ->
        lines.any { it.startsWith(COMPOSE_IMPORT) }
    }

    private companion object {
        const val COMPOSE_PACKAGE: String = "androidx.compose"
        const val COMPOSE_IMPORT: String = "import $COMPOSE_PACKAGE"
    }
}
