package com.emm.justchill.loans.domain

interface LoanRepository {

    suspend fun add(loan: Loan)
}