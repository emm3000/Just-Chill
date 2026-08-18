package com.emm.data.shared

expect fun Throwable.isSqliteConstraintViolation(): Boolean

expect fun Throwable.isSqliteException(): Boolean
