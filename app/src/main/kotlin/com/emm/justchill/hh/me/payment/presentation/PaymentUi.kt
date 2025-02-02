package com.emm.justchill.hh.me.payment.presentation

import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.me.payment.domain.Payment
import com.emm.justchill.hh.me.payment.domain.PaymentStatus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

data class PaymentUi(
    val paymentId: String,
    val loanId: String,
    val dueDate: Long,
    val amount: String,
    val status: PaymentStatus,
    val day: String,
    val dayNumber: String,
)

fun Payment.toUi(): PaymentUi {

    val date: LocalDateTime = LocalDateTime
        .ofInstant(Instant.ofEpochMilli(dueDate), ZoneId.systemDefault())
    return PaymentUi(
        paymentId = paymentId,
        loanId = loanId,
        dueDate = dueDate,
        amount = "S/ ${fromCentsToSolesWith(amount)}",
        status = status,
        day = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es")).uppercase(),
        dayNumber = date.dayOfMonth.toString()

    )
}