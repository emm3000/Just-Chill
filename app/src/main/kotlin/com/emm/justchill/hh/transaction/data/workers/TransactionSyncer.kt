package com.emm.justchill.hh.transaction.data.workers

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.emm.justchill.hh.shared.Syncer

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