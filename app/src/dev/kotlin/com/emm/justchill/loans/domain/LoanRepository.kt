package com.emm.justchill.loans.domain

import kotlinx.coroutines.flow.Flow

interface LoanRepository {

    suspend fun add(loan: Loan)

    fun retrieveBy(loanId: String): Flow<List<Loan>>

    fun all(): Flow<List<Loan>>

    fun retrieveByDriverId(driverId: Long): Flow<List<Loan>>

    suspend fun delete(loanId: String)
}