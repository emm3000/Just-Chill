package com.emm.justchill.loans.domain

import com.emm.justchill.hh.data.shared.DefaultUniqueIdProvider
import com.emm.justchill.hh.domain.shared.UniqueIdProvider
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.round

class PaymentsGenerator(private val uniqueIdProvider: UniqueIdProvider = DefaultUniqueIdProvider) {

    fun generate(loanCreate: LoanCreate): List<Payment> {
        checkNotNull(loanCreate.loanId)
        val loanId: String = loanCreate.loanId

        val payments: MutableList<Payment> = mutableListOf()
        val duration: Int = loanCreate.durationAtNumber.toInt()
        val amountWithInterest: Double = loanCreate.amountWithInterest
        val exactQuote: Double = floor(amountWithInterest / duration)

        var internalStartDate: LocalDate = loanCreate.startDateAtLocalDate
        var accumulatedAmount = 0.0
        var paymentCount = 0

        while (paymentCount < duration - 1) {
            internalStartDate = internalStartDate.plusOneDay()

            if (internalStartDate.dayOfWeek == DayOfWeek.SUNDAY) {
                continue
            }

            val payment = Payment(
                paymentId = uniqueIdProvider.uniqueId,
                loanId = loanId,
                dueDate = internalStartDate.toMillis(),
                amount = exactQuote,
                status = PaymentStatus.PENDING.name,
            )
            payments.add(payment)
            accumulatedAmount += exactQuote
            paymentCount++
        }

        addLastQuote(
            internalStartDate = internalStartDate,
            totalAmount = amountWithInterest,
            accumulatedAmount = accumulatedAmount,
            paymentModels = payments,
            loanId = loanId
        )

        return payments
    }

    private fun addLastQuote(
        internalStartDate: LocalDate,
        totalAmount: Double,
        accumulatedAmount: Double,
        paymentModels: MutableList<Payment>,
        loanId: String,
    ) {

        val lastQuota = round(totalAmount - accumulatedAmount)

        val lastQuote = Payment(
            paymentId = uniqueIdProvider.uniqueId,
            loanId = loanId,
            dueDate = internalStartDate.plusOneDay().toMillis(),
            amount = lastQuota,
            status = PaymentStatus.PENDING.name,
        )
        paymentModels.add(lastQuote)
    }

    private fun LocalDate.plusOneDay(): LocalDate = plusDays(1)

    private fun LocalDate.toMillis(): Long = this
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}