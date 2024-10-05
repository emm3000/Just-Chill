package com.emm.justchill.hh.shared

import com.emm.justchill.hh.transaction.domain.SyncStatus
import com.google.firebase.crashlytics.FirebaseCrashlytics

fun syncStatus(syncStatus: String): SyncStatus? {
    return try {
        SyncStatus.valueOf(syncStatus)
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        null
    }
}