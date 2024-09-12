package com.emm.justchill.hh.data.transaction.workers

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.emm.justchill.hh.data.shared.Syncer

class TransactionSyncer(
    private val context: Context,
): Syncer {

    override fun sync(id: String) {
        WorkManager.getInstance(context).apply {
            enqueueUniqueWork(
                id,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                TransactionWorker.startUpSyncWork(id),
            )
        }
    }
}