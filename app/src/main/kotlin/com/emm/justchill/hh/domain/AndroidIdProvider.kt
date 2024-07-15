package com.emm.justchill.hh.domain

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings.Secure

class AndroidIdProvider(private val context: Context) {

    @SuppressLint("HardwareIds")
    fun androidId(): String = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
}