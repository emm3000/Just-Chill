package com.emm.justchill.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration

fun provideSqlDriver(): SqlDriver = NativeSqliteDriver(
    DatabaseConfiguration(
        name = DATABASE_NAME,
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
    ),
)
