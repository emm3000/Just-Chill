package com.emm.justchill.loans.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.justchill.EmmDatabase
import com.emm.justchill.Loans
import com.emm.justchill.LoansQueries
import com.emm.justchill.loans.domain.Loan
import com.emm.justchill.loans.domain.LoanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalLoanRepository(
    private val emmDatabase: EmmDatabase,
): LoanRepository {

    private val lq: LoansQueries
        get() = emmDatabase.loansQueries

    override suspend fun add(loan: Loan) = withContext(Dispatchers.IO) {
        lq.insert(
            loanId = loan.loanId,
            amount = loan.amount,
            amountWithInterest = loan.amountWithInterest,
            interest = loan.interest,
            startDate = loan.startDate,
            duration = loan.duration,
            status = loan.status,
            driverId = loan.driverId,
        )
    }

    override fun retrieveBy(loanId: String): Flow<List<Loan>> {
        return lq.find(loanId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(::toDomain)
    }

    override fun retrieveByDriverId(driverId: Long): Flow<List<Loan>> {
        return lq.findByDriver(driverId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(::toDomain)
    }

    private fun toDomain(loans: List<Loans>): List<Loan> {
        return loans.map {
            Loan(
                loanId = it.loanId,
                amount = it.amount,
                amountWithInterest = it.amountWithInterest,
                interest = it.interest,
                startDate = it.startDate,
                duration = it.duration,
                status = it.status,
                driverId = it.driverId,
            )
        }
    }
}