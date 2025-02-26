package com.emm.justchill.hh.me.loan.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class LoanCreate(
    val loanId: String? = null,
    val amount: String,
    val interest: String,
    val startDate: Long,
    val duration: String,
    val driverId: Long,
    val frequencyType: FrequencyType,
) {

    private val amountAtNumber: Double
        get() = amount.toDoubleOrNull() ?: throw IllegalArgumentException()

    private val interestAtNumber: Double
        get() = interest.toDoubleOrNull() ?: throw IllegalArgumentException()

    val durationAtNumber: Long
        get() = duration.toLongOrNull() ?: throw IllegalArgumentException()

    val amountWithInterest: Double
        get() = amountAtNumber + (amountAtNumber * interestAtNumber / 100)

    val startDateAtLocalDate: LocalDate
        get() = Instant.ofEpochMilli(startDate)
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()
}