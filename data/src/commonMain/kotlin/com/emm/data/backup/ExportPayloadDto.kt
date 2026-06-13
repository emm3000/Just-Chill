package com.emm.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class ExportPayloadDto(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val appVersion: String,
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
)
