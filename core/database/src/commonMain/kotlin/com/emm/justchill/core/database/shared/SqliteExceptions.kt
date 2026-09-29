package com.emm.justchill.core.database.shared

expect fun Throwable.isSqliteConstraintViolation(): Boolean

expect fun Throwable.isSqliteException(): Boolean
