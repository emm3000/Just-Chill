package com.emm.data.shared

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException

actual fun Throwable.isSqliteConstraintViolation(): Boolean = this is SQLiteConstraintException

actual fun Throwable.isSqliteException(): Boolean = this is SQLiteException
