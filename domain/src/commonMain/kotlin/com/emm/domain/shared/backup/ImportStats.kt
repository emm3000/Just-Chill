package com.emm.domain.shared.backup

data class ImportStats(
    val accounts: Int,
    val categories: Int,
    val transactions: Int,
    val recurring: Int,
    val loans: Int,
    val loanPayments: Int,
)
