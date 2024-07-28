package com.emm.justchill

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.emm.justchill.core.coreModule
import com.emm.justchill.experiences.drinks.drinkModule
import com.emm.justchill.experiences.readjsonfromassets.experiencesModule
import com.emm.justchill.experiences.supabase.supabaseModule
import com.emm.justchill.hh.hhModule
import com.emm.justchill.loans.loansModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

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
                hhModule,
                supabaseModule,
                loansModule,
            )
        }
    }

    private fun initWorkers() {
        Sync.initialize(applicationContext)
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