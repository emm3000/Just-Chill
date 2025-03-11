package com.emm.justchill

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.emm.justchill.core.coreModule
import com.emm.justchill.experiences.readjsonfromassets.experiencesModule
import com.emm.justchill.hh.di.accountModule
import com.emm.justchill.hh.di.categoryModule
import com.emm.justchill.hh.di.dbModule
import com.emm.justchill.hh.di.hhModule
import com.emm.justchill.hh.di.loansModule
import com.emm.justchill.hh.di.transactionModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class EmmApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startKoin {
            androidLogger()
            androidContext(this@EmmApp)
            modules(
                coreModule,
                experiencesModule,
                hhModule,
                loansModule,
                categoryModule,
                accountModule,
                transactionModule,
                dbModule,
            )
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "backup_channel",
            "Backup Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "Channel for backup notifications"
        val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}