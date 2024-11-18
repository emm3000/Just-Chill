package com.emm.justchill.hh.shared.shared

import com.emm.justchill.hh.transaction.presentation.TransactionType
import kotlinx.serialization.Serializable

@Serializable
object Main

@Serializable
data class EditTransaction(val transactionId: String)

@Serializable
object Category

@Serializable
object Account

@Serializable
data class FastTransaction(val accountId: String, val transactionType: TransactionType)