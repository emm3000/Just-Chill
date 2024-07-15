package com.emm.justchill.hh.domain

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings.Secure

class AndroidDataProvider(private val context: Context) {

    @SuppressLint("HardwareIds")
    fun androidId(): String = Secure.getString(context.contentResolver, Secure.ANDROID_ID)

    fun deviceName(): String = "${Build.MANUFACTURER}-${Build.MODEL}"
}