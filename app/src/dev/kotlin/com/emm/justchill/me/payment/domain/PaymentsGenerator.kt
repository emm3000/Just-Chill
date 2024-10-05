package com.emm.justchill.me.payment.domain

import com.emm.justchill.hh.shared.DefaultUniqueIdProvider
import com.emm.justchill.hh.shared.UniqueIdProvider
import com.emm.justchill.me.loan.domain.LoanCreate
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
        val exactDaily: Double = floor(amountWithInterest / duration)

        var internalStartDate: LocalDate = loanCreate.startDateAtLocalDate.minusDays(1)
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
                amount = exactDaily,
                status = PaymentStatus.PENDING,
            )
            payments.add(payment)
            accumulatedAmount += exactDaily
            paymentCount++
        }

        addLastDaily(
            internalStartDate = internalStartDate,
            totalAmount = amountWithInterest,
            accumulatedAmount = accumulatedAmount,
            paymentModels = payments,
            loanId = loanId
        )

        return payments
    }

    private fun addLastDaily(
        internalStartDate: LocalDate,
        totalAmount: Double,
        accumulatedAmount: Double,
        paymentModels: MutableList<Payment>,
        loanId: String,
    ) {

        val lastDailyAmount = round(totalAmount - accumulatedAmount)

        var lastDailyDate = internalStartDate.plusOneDay()

        if (lastDailyDate.dayOfWeek == DayOfWeek.SUNDAY) {
            lastDailyDate = lastDailyDate.plusOneDay()
        }

        val lastDaily = Payment(
            paymentId = uniqueIdProvider.uniqueId,
            loanId = loanId,
            dueDate = lastDailyDate.toMillis(),
            amount = lastDailyAmount,
            status = PaymentStatus.PENDING,
        )
        paymentModels.add(lastDaily)
    }

    private fun LocalDate.plusOneDay(): LocalDate = plusDays(1)

    private fun LocalDate.toMillis(): Long = this
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}