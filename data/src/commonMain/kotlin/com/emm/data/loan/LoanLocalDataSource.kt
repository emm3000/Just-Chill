package com.emm.data.loan

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.EmmDatabaseData
import com.emm.data.LoansQueries
import com.emm.data.shared.ioDispatcher
import com.emm.data.shared.nowMillis
import com.emm.data.shared.toOccurredAtText
import com.emm.domain.loan.Loan
import com.emm.domain.loan.LoanBalance
import com.emm.domain.loan.PersonBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class LoanLocalDataSource(private val emmDatabase: EmmDatabaseData, private val clock: Clock) {

    private val lq: LoansQueries
        get() = emmDatabase.loansQueries

    fun byId(loanId: String): Flow<Loan?> = lq.byId(loanId)
        .asFlow()
        .mapToOneOrNull(ioDispatcher)
        .map { row -> row?.asEntity()?.asExternalModelOrNull() }

    fun loansWithBalance(personKey: String): Flow<List<LoanBalance>> = lq.loansWithBalance(personKey)
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.asLoanBalances() }

    fun balancesByPerson(): Flow<List<PersonBalance>> = lq.balancesByPerson()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.map { it.asExternalModel() } }

    suspend fun create(loan: Loan) = withContext(ioDispatcher) {
        val now = clock.nowMillis()
        lq.insert(
            loanId = loan.id.value,
            personName = loan.personName,
            personKey = loan.personKey,
            principal = loan.principal.cents,
            interestBps = loan.interestBps.toLong(),
            totalDue = loan.totalDue.cents,
            note = loan.note,
            lentAt = loan.lentAt.toOccurredAtText(),
            createdAt = now,
            updatedAt = now,
        )
    }

    suspend fun update(loan: Loan) = withContext(ioDispatcher) {
        lq.updateValues(
            personName = loan.personName,
            personKey = loan.personKey,
            principal = loan.principal.cents,
            interestBps = loan.interestBps.toLong(),
            totalDue = loan.totalDue.cents,
            note = loan.note,
            lentAt = loan.lentAt.toOccurredAtText(),
            updatedAt = clock.nowMillis(),
            loanId = loan.id.value,
        )
    }

    suspend fun softDelete(loanId: String) = withContext(ioDispatcher) {
        val now = clock.nowMillis()
        emmDatabase.transaction {
            emmDatabase.loansQueries.softDelete(deletedAt = now, updatedAt = now, loanId = loanId)
            emmDatabase.loan_paymentsQueries.softDeleteByLoan(deletedAt = now, updatedAt = now, loanId = loanId)
        }
    }
}
