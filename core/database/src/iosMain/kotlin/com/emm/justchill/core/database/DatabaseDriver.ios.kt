package com.emm.justchill.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.createDatabaseManager
import co.touchlab.sqliter.withConnection

fun provideSqlDriver(): SqlDriver = openSqlDriver(databaseConfiguration(DATABASE_NAME))

internal fun databaseConfiguration(name: String): DatabaseConfiguration = DatabaseConfiguration(
    name = name,
    version = JustChillDatabase.Schema.version.toInt(),
    create = { connection ->
        wrapConnection(connection) { driver ->
            JustChillDatabase.Schema.create(driver)
            driver.execute(identifier = null, sql = SEED_DEFAULT_CATEGORIES, parameters = 0)
        }
    },
    upgrade = { connection, oldVersion, newVersion ->
        wrapConnection(connection) { driver ->
            JustChillDatabase.Schema.migrate(driver, oldVersion.toLong(), newVersion.toLong())
        }
    },
    extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true),
)

internal fun openSqlDriver(configuration: DatabaseConfiguration): SqlDriver {
    migrateWithForeignKeysOff(configuration)
    return NativeSqliteDriver(configuration)
}

// SQLiter turns foreign keys on before it runs create or upgrade; Android turns them on in onOpen, after.
private fun migrateWithForeignKeysOff(configuration: DatabaseConfiguration) {
    val foreignKeysOff: DatabaseConfiguration = configuration.copy(
        extendedConfig = configuration.extendedConfig.copy(foreignKeyConstraints = false),
    )
    createDatabaseManager(foreignKeysOff).withConnection { }
}
