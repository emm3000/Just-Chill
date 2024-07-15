package com.emm.justchill

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.emm.justchill.hh.presentation.BackupWorker

object Sync {

    fun initialize(context: Context) {

        WorkManager.getInstance(context).apply {
            enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                BackupWorker.startUpSyncWork(),
            )
        }
    }
}

internal const val SYNC_WORK_NAME = "midnightWorker"
