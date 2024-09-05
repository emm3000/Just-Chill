package com.emm.justchill.hh.domain.transaction.remote

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.emm.justchill.hh.data.workers.TransactionAddedWorker

class TransactionAdderResolver(
    private val context: Context,
) {

    fun resolve(transactionId: String) {

        WorkManager.getInstance(context).apply {
            enqueueUniqueWork(
                transactionId,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                TransactionAddedWorker.startUpSyncWork(transactionId),
            )
        }
    }
}