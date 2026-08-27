// detekt 2.0.0-alpha.6 misresolves `withContext` (an inline suspend fun) under AGP 9's
// built-in-Kotlin compile and flags every suspend fun here that only wraps one as redundant;
// removing suspend/withContext would run these DB writes off the caller's dispatcher instead of
// IO. detektMainAndroid (KMP) never saw this on byte-identical source before E11-05 — a known
// detekt false-positive class: https://github.com/detekt/detekt/issues/8019.
@file:Suppress("RedundantSuspendModifier")

package com.emm.data.loan

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.EmmDatabaseData
import com.emm.data.Loan_paymentsQueries
import com.emm.data.shared.ioDispatcher
import com.emm.data.shared.nowMillis
import com.emm.data.shared.toOccurredAtText
import com.emm.domain.loan.LoanPayment
import com.emm.domain.shared.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class LoanPaymentLocalDataSource(private val emmDatabase: EmmDatabaseData, private val clock: Clock) {

    private val lpq: Loan_paymentsQueries
        get() = emmDatabase.loan_paymentsQueries

    fun byId(paymentId: String): Flow<LoanPayment?> = lpq.byId(paymentId)
        .asFlow()
        .mapToOneOrNull(ioDispatcher)
        .map { row -> row?.asEntity()?.asExternalModelOrNull() }

    fun byLoan(loanId: String): Flow<List<LoanPayment>> = lpq.byLoan(loanId)
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.asEntity().asExternalModel() }

    suspend fun paidSoFar(loanId: String): Money = withContext(ioDispatcher) {
        Money(lpq.paidSoFar(loanId).executeAsOne())
    }

    suspend fun create(loanPayment: LoanPayment) = withContext(ioDispatcher) {
        val now = clock.nowMillis()
        lpq.insert(
            paymentId = loanPayment.id.value,
            loanId = loanPayment.loanId.value,
            amount = loanPayment.amount.cents,
            method = loanPayment.method.name,
            paidAt = loanPayment.paidAt.toOccurredAtText(),
            note = loanPayment.note,
            createdAt = now,
            updatedAt = now,
        )
    }

    suspend fun update(loanPayment: LoanPayment) = withContext(ioDispatcher) {
        lpq.update(
            amount = loanPayment.amount.cents,
            method = loanPayment.method.name,
            paidAt = loanPayment.paidAt.toOccurredAtText(),
            note = loanPayment.note,
            updatedAt = clock.nowMillis(),
            paymentId = loanPayment.id.value,
        )
    }

    suspend fun softDelete(paymentId: String) = withContext(ioDispatcher) {
        val now = clock.nowMillis()
        lpq.softDelete(deletedAt = now, updatedAt = now, paymentId = paymentId)
    }
}
