package com.emm.justchill.hh.data.workers

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.emm.justchill.hh.domain.transaction.TransactionSyncer

class DefaultTransactionSyncer(
    private val context: Context,
): TransactionSyncer {

    override fun sync(transactionId: String) {
        WorkManager.getInstance(context).apply {
            enqueueUniqueWork(
                transactionId,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                TransactionAddedWorker.startUpSyncWork(transactionId),
            )
        }
    }
}