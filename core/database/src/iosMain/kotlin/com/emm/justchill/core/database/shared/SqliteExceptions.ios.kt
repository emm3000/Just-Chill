package com.emm.justchill.core.database.shared

import co.touchlab.sqliter.interop.SQLiteException
import co.touchlab.sqliter.interop.SQLiteExceptionErrorCode
import co.touchlab.sqliter.interop.SqliteErrorType

actual fun Throwable.isSqliteConstraintViolation(): Boolean =
    this is SQLiteExceptionErrorCode && errorType == SqliteErrorType.SQLITE_CONSTRAINT

actual fun Throwable.isSqliteException(): Boolean = this is SQLiteException
