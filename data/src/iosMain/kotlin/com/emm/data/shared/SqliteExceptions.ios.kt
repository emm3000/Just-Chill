package com.emm.data.shared

// Best-effort: the SQLDelight native driver surfaces SQLite errors with the engine's
// message text. Matched by message to avoid importing an uncertain native exception FQN.
// TODO(phase6): verify against the real co.touchlab.sqliter exception on an iOS device.
actual fun Throwable.isSqliteConstraintViolation(): Boolean =
    message?.contains("constraint", ignoreCase = true) == true

actual fun Throwable.isSqliteException(): Boolean =
    message?.contains("sqlite", ignoreCase = true) == true ||
        (this::class.simpleName?.contains("SQL", ignoreCase = true) == true)
