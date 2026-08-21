package com.emm.data.loan

import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.safeDbCall
import com.emm.domain.loan.LoanPayment
import com.emm.domain.loan.LoanPaymentRepository
import com.emm.domain.shared.LoanId
import com.emm.domain.shared.LoanPaymentId
import com.emm.domain.shared.Money
import kotlinx.coroutines.flow.Flow

class DefaultLoanPaymentRepository(private val localDataSource: LoanPaymentLocalDataSource) : LoanPaymentRepository {

    override fun byLoan(loanId: LoanId): Flow<List<LoanPayment>> =
        localDataSource.byLoan(loanId.value).catchAsDomainException()

    override suspend fun paidSoFar(loanId: LoanId): Money = safeDbCall {
        localDataSource.paidSoFar(loanId.value)
    }

    override suspend fun create(loanPayment: LoanPayment): Unit = safeDbCall {
        localDataSource.create(loanPayment)
        Unit
    }

    override suspend fun delete(loanPaymentId: LoanPaymentId): Unit = safeDbCall {
        localDataSource.softDelete(loanPaymentId.value)
    }
}
