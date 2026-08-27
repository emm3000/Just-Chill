package com.emm.domain.loan

import com.emm.domain.shared.LoanId
import com.emm.domain.shared.LoanPaymentId
import com.emm.domain.shared.Money
import kotlinx.coroutines.flow.Flow

interface LoanPaymentRepository {

    fun byLoan(loanId: LoanId): Flow<List<LoanPayment>>

    fun byId(loanPaymentId: LoanPaymentId): Flow<LoanPayment?>

    suspend fun paidSoFar(loanId: LoanId): Money

    suspend fun create(loanPayment: LoanPayment)

    suspend fun update(loanPayment: LoanPayment)

    suspend fun delete(loanPaymentId: LoanPaymentId)
}
