package com.emm.justchill.hh.shared.shared

import com.emm.domain.transaction.TransactionType
import kotlinx.serialization.Serializable

@Serializable
data class EditTransactionRoute(val transactionId: String)

@Serializable
object CategoryRoute

@Serializable
object AccountRoute

@Serializable
data class FastTransactionRoute(val accountId: String, val transactionType: TransactionType)