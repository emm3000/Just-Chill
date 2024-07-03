package com.emm.justchill

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.emm.justchill.core.coreModule
import com.emm.justchill.experiences.drinks.drinkModule
import com.emm.justchill.experiences.hh.hhModule
import com.emm.justchill.experiences.hh.presentation.BackupWorker
import com.emm.justchill.experiences.readjsonfromassets.experiencesModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class EmmApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initWorkers()
        createNotificationChannel()
        startKoin {
            androidLogger()
            androidContext(this@EmmApp)
            modules(
                coreModule,
                drinkModule,
                experiencesModule,
                hhModule
            )
        }
    }

    private fun initWorkers() {
        enqueueMidnightWorker(applicationContext)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "backup_channel",
            "Backup Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Channel for backup notifications"
        }
        val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        fun enqueueMidnightWorker(context: Context) {
            val currentTime: LocalDateTime = LocalDateTime.now()
            val midnight: LocalDateTime = currentTime.toLocalDate().atTime(LocalTime.MIDNIGHT)
            val delay: Long = midnight.atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli() - System.currentTimeMillis()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<BackupWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            ).setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "midnightWorker",
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )
        }
    }
}