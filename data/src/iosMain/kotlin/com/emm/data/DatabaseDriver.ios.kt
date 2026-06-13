package com.emm.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration

// Filename MUST stay exactly "com.emm.data.db" to match the Android driver
// (DatabaseDriver.android.kt). Same name keeps the on-device DB path consistent
// across platforms.
private const val DATABASE_NAME = "com.emm.data.db"

// iOS counterpart of provideSqlDriver(context) in DatabaseDriver.android.kt.
//
// NativeSqliteDriver (SQLDelight 2.x) already wires schema.create / schema.migrate
// through the SqlSchema it receives, so we only need to replicate the Android
// Callback's onOpen behavior: enable SQLite foreign-key enforcement. We do NOT
// hook create/upgrade lambdas here — those belong to SQLDelight's internal wiring,
// and overriding them via onConfiguration would clobber schema creation.
//
// Default-category seeding (the Android onCreate path) is deferred to slice 5b:
// 5a proves the native driver links and SQLDelight queries run; an empty
// transactions list is a valid local-first empty state. // TODO 5b: seed default
// categories on first create for iOS parity with Android.
fun provideSqlDriver(): SqlDriver = NativeSqliteDriver(
    schema = EmmDatabaseData.Schema,
    name = DATABASE_NAME,
    onConfiguration = { config: DatabaseConfiguration ->
        config.copy(
            extendedConfig = config.extendedConfig.copy(foreignKeyConstraints = true),
        )
    },
)
