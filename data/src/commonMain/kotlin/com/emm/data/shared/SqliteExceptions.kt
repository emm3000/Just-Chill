package com.emm.data.shared

/** True if [this] is a SQLite constraint violation (e.g. FK/unique). */
expect fun Throwable.isSqliteConstraintViolation(): Boolean

/** True if [this] is any SQLite-layer failure. */
expect fun Throwable.isSqliteException(): Boolean
