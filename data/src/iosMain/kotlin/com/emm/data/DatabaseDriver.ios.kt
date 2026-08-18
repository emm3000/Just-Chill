package com.emm.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration

private const val DATABASE_NAME = "com.emm.data.db"

/**
 * NativeSqliteDriver wires schema.create and schema.migrate itself; onConfiguration must extend the
 * config it receives, never replace it, or schema creation is clobbered.
 */
fun provideSqlDriver(): SqlDriver = NativeSqliteDriver(
    schema = EmmDatabaseData.Schema,
    name = DATABASE_NAME,
    onConfiguration = { config: DatabaseConfiguration ->
        config.copy(
            extendedConfig = config.extendedConfig.copy(foreignKeyConstraints = true),
        )
    },
)
