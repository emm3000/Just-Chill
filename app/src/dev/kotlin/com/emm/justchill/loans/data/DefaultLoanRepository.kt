package com.emm.justchill.loans.data

import android.util.Log
import com.emm.justchill.loans.domain.Loan
import com.emm.justchill.loans.domain.LoanRepository

class DefaultLoanRepository: LoanRepository {

    override suspend fun add(loan: Loan) {
        Log.e("aea", loan.toString())
    }
}