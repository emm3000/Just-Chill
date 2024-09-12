package com.emm.justchill.hh.data.shared

import com.emm.justchill.hh.domain.transaction.SyncStatus
import com.google.firebase.crashlytics.FirebaseCrashlytics

fun syncStatus(syncStatus: String): SyncStatus? {
    return try {
        SyncStatus.valueOf(syncStatus)
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        null
    }
}