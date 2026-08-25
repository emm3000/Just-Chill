package com.emm.data.shared

actual fun Throwable.isSqliteConstraintViolation(): Boolean = message?.contains("constraint", ignoreCase = true) == true

actual fun Throwable.isSqliteException(): Boolean = message?.contains("sqlite", ignoreCase = true) == true ||
    (this::class.simpleName?.contains("SQL", ignoreCase = true) == true)
