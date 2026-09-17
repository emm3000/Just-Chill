package com.emm.buildlogic

import app.cash.sqldelight.gradle.SqlDelightExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

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
    }

    private companion object {
        const val DATABASE_NAME: String = "EmmDatabaseData"
        const val DATABASE_PACKAGE: String = "com.emm.data"
        const val SCHEMA_DIRECTORY: String = "src/main/sqldelight/databases"
    }
}
