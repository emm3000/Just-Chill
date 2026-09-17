package com.emm.justchill.core.database.loan

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.justchill.core.database.JustChillDatabase
import com.emm.justchill.core.database.Loan_paymentsQueries
import com.emm.justchill.core.database.shared.ioDispatcher
import com.emm.justchill.core.database.shared.nowMillis
import com.emm.justchill.core.database.shared.toOccurredAtText
import com.emm.justchill.core.domain.loan.LoanPayment
import com.emm.justchill.core.domain.shared.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class LoanPaymentLocalDataSource(private val emmDatabase: JustChillDatabase, private val clock: Clock) {

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
