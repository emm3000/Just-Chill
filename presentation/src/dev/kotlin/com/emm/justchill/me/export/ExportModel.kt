package com.emm.justchill.me.export

import kotlinx.serialization.Serializable

@Serializable
data class ExportModel(
    val drivers: List<DriverModel>,
    val payments: List<PaymentModel>,
    val loans: List<LoanModel>,
    val daily: List<DailyModel>,
)

@Serializable
data class DriverModel(
    val driverId: Long,
    val name: String,
)

@Serializable
data class PaymentModel(
    val paymentId: String,
    val loanId: String,
    val dueDate: Long,
    val amount: Double,
    val status: String,
)

@Serializable
data class LoanModel(
    val loanId: String,
    val amount: Double,
    val amountWithInterest: Double,
    val interest: Long,
    val startDate: Long,
    val duration: Long,
    val status: String,
    val driverId: Long,
)

@Serializable
data class DailyModel(
    val dailyId: String,
    val amount: Double,
    val dailyDate: Long,
    val driverId: Long,
)