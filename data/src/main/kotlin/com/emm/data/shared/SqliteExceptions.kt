package com.emm.data.shared

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException

fun Throwable.isSqliteConstraintViolation(): Boolean = this is SQLiteConstraintException

fun Throwable.isSqliteException(): Boolean = this is SQLiteException
