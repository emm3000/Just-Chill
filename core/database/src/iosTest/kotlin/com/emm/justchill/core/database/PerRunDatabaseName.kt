package com.emm.justchill.core.database

import platform.Foundation.NSUUID

internal fun perRunDatabaseName(role: String): String = "$role-${NSUUID().UUIDString}"
