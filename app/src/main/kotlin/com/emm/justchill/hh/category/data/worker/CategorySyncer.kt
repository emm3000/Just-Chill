package com.emm.justchill.hh.category.data.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.emm.justchill.hh.shared.Syncer

class CategorySyncer(private val context: Context) : Syncer {

    override fun sync(id: String) {
        WorkManager.getInstance(context).apply {
            enqueueUniqueWork(
                id,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                CategoryWorker.startUpSyncWork(id),
            )
        }
    }
}