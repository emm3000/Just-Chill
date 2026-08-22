package com.emm.data.backup

import kotlinx.serialization.Serializable

const val BACKUP_SCHEMA_VERSION: Int = 4

@Serializable
data class ExportPayloadDto(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val exportedAt: Long,
    val appVersion: String,
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
    val recurringMovements: List<RecurringMovementDto>,
    val loans: List<LoanDto>,
    val loanPayments: List<LoanPaymentDto>,
)
