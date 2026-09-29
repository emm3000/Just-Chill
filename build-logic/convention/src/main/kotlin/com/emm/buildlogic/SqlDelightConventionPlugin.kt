package com.emm.buildlogic

import app.cash.sqldelight.gradle.SqlDelightExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import java.io.File

class SqlDelightConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("app.cash.sqldelight")

        extensions.configure<SqlDelightExtension> {
            databases.create(DATABASE_NAME) {
                packageName.set(DATABASE_PACKAGE)
                schemaOutputDirectory.set(file(SCHEMA_DIRECTORY))
                verifyMigrations.set(true)
            }
        }

        stageSnapshotsForTheSimulator()
    }

    private fun Project.stageSnapshotsForTheSimulator() {
        // The simulator cannot read a checkout under a TCC-protected folder such as ~/Documents.
        val stagedSnapshots: File = File(
            System.getProperty(TEMPORARY_DIRECTORY_PROPERTY),
            "$STAGED_SNAPSHOTS_PREFIX${layout.projectDirectory.asFile.absolutePath.hashCode()}",
        )
        val stage: TaskProvider<Sync> = tasks.register<Sync>(STAGE_TASK) {
            from(layout.projectDirectory.dir(SCHEMA_DIRECTORY))
            into(stagedSnapshots)
        }
        tasks.withType<KotlinNativeSimulatorTest>().configureEach {
            inputs.files(stage).withPathSensitivity(PathSensitivity.RELATIVE)
            environment(SNAPSHOTS_ENVIRONMENT_VARIABLE, stagedSnapshots.absolutePath, tracked = false)
        }
    }

    companion object {
        const val SCHEMA_DIRECTORY: String = "src/commonMain/sqldelight/databases"
        private const val DATABASE_NAME: String = "JustChillDatabase"
        private const val DATABASE_PACKAGE: String = "com.emm.justchill.core.database"
        private const val STAGE_TASK: String = "stageSqlDelightSnapshots"
        private const val TEMPORARY_DIRECTORY_PROPERTY: String = "java.io.tmpdir"
        private const val STAGED_SNAPSHOTS_PREFIX: String = "justchill-sqldelight-snapshots-"
        private const val SNAPSHOTS_ENVIRONMENT_VARIABLE: String = "SIMCTL_CHILD_SQLDELIGHT_SNAPSHOTS"
    }
}
