package com.emm.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
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

    @get:Input
    @get:Optional
    abstract val floor: Property<Int>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val migrated: List<Int> = versionsOf(migrations)
        val captured: List<Int> = versionsOf(snapshots)
        val reportFile: File = report.get().asFile
        reportFile.parentFile.mkdirs()

        if (migrated.isEmpty() && captured.isEmpty()) {
            reportFile.writeText("")
            return
        }

        val pinned: Int = floor.orNull ?: throw GradleException(failure(listOf(UNPINNED)))
        val problems: List<String> = problems(migrated, captured, pinned)

        if (problems.isNotEmpty()) throw GradleException(failure(problems))

        reportFile.writeText(pairs(migrated, pinned).joinToString(separator = "\n", postfix = "\n"))
    }

    private fun failure(problems: List<String>): String =
        problems.joinToString(separator = "\n", prefix = MISMATCH, postfix = "\n$FIX")

    private fun problems(migrated: List<Int>, captured: List<Int>, pinned: Int): List<String> =
        buildList {
            if (pinned !in captured) add("the pinned floor $pinned.db is missing")
            migrated
                .filter { it + 1 >= pinned && it + 1 !in captured }
                .forEach { add("$it.sqm has no snapshot ${it + 1}.db") }
            captured
                .filter { it != pinned && it - 1 !in migrated }
                .forEach { add("$it.db has no migration ${it - 1}.sqm") }
        }

    private fun pairs(migrated: List<Int>, pinned: Int): List<String> = migrated
        .filter { it + 1 >= pinned }
        .map { "$it.sqm ${it + 1}.db" }

    private fun versionsOf(files: ConfigurableFileCollection): List<Int> = files.files
        .filter { it.isFile }
        .mapNotNull { it.nameWithoutExtension.toIntOrNull() }
        .sorted()

    private companion object {
        const val MISMATCH: String = "SQLDelight migrations and schema snapshots do not match:\n"
        const val UNPINNED: String =
            "this module has SQLDelight schema sources but no pinned snapshot floor"
        const val FIX: String =
            "Pin the oldest committed snapshot with sqlDelightSnapshots { floor.set(N) } and generate " +
                "a missing one with ./gradlew :core:database:generateDebugJustChillDatabaseSchema"
    }
}
