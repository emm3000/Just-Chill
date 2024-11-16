package com.emm.justchill.hh.shared.shared

import kotlinx.serialization.Serializable

@Serializable
object Main

@Serializable
data class EditTransaction(val transactionId: String)

@Serializable
object Category

@Serializable
object Account