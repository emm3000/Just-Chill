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

abstract class CheckSqlDelightSnapshotsTask : DefaultTask() {

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val migrations: ConfigurableFileCollection

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val snapshots: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val migrated: List<Int> = versionsOf(migrations)
        val captured: List<Int> = versionsOf(snapshots)
        val baseline: Int = captured.minOrNull() ?: Int.MIN_VALUE
        val problems: List<String> = problems(migrated, captured, baseline)

        if (problems.isNotEmpty()) {
            throw GradleException(
                problems.joinToString(separator = "\n", prefix = MISMATCH, postfix = "\n$FIX"),
            )
        }

        val reportFile: File = report.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(pairs(migrated, baseline).joinToString(separator = "\n", postfix = "\n"))
    }

    private fun problems(migrated: List<Int>, captured: List<Int>, baseline: Int): List<String> =
        buildList {
            migrated
                .filter { it + 1 >= baseline && it + 1 !in captured }
                .forEach { add("$it.sqm has no snapshot ${it + 1}.db") }
            captured
                .filter { it != baseline && it - 1 !in migrated }
                .forEach { add("$it.db has no migration ${it - 1}.sqm") }
        }

    private fun pairs(migrated: List<Int>, baseline: Int): List<String> = migrated
        .filter { it + 1 >= baseline }
        .map { "$it.sqm ${it + 1}.db" }

    private fun versionsOf(files: ConfigurableFileCollection): List<Int> = files.files
        .filter { it.isFile }
        .mapNotNull { it.nameWithoutExtension.toIntOrNull() }
        .sorted()

    private companion object {
        const val MISMATCH: String = "SQLDelight migrations and schema snapshots do not match:\n"
        const val FIX: String =
            "Generate the snapshot with ./gradlew :core:database:generateDebugJustChillDatabaseSchema"
    }
}
