package com.emm.justchill.hh.domain

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings.Secure
import com.emm.justchill.hh.domain.TransactionModel

class TransactionModelFactory(private val context: Context) {

    @SuppressLint("HardwareIds")
    fun create(transactionModel: TransactionModel): TransactionModel {
        val androidId: String = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
        return transactionModel.copy(deviceId = androidId)
    }

    @SuppressLint("HardwareIds")
    fun androidId(): String = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
}