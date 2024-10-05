package com.emm.justchill.hh.account.data.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.emm.justchill.hh.shared.Syncer

class AccountSyncer(private val context: Context): Syncer {

    override fun sync(id: String) {
        WorkManager.getInstance(context).apply {
            enqueueUniqueWork(
                id,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                AccountWorker.startUpSyncWork(id),
            )
        }
    }
}