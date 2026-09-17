package com.emm.justchill.core.database.loan

import com.emm.justchill.core.database.shared.catchAsDomainException
import com.emm.justchill.core.database.shared.safeDbCall
import com.emm.justchill.core.domain.loan.Loan
import com.emm.justchill.core.domain.loan.LoanBalance
import com.emm.justchill.core.domain.loan.LoanRepository
import com.emm.justchill.core.domain.loan.PersonBalance
import com.emm.justchill.core.domain.shared.LoanId
import kotlinx.coroutines.flow.Flow

class DefaultLoanRepository(private val localDataSource: LoanLocalDataSource) : LoanRepository {

    override fun balancesByPerson(): Flow<List<PersonBalance>> =
        localDataSource.balancesByPerson().catchAsDomainException()

    override fun loansWithBalance(personKey: String): Flow<List<LoanBalance>> =
        localDataSource.loansWithBalance(personKey).catchAsDomainException()

    override fun byId(loanId: LoanId): Flow<Loan?> = localDataSource.byId(loanId.value).catchAsDomainException()

    override suspend fun create(loan: Loan): Unit = safeDbCall {
        localDataSource.create(loan)
        Unit
    }

    override suspend fun update(loan: Loan): Unit = safeDbCall {
        localDataSource.update(loan)
        Unit
    }

    override suspend fun delete(loanId: LoanId): Unit = safeDbCall {
        localDataSource.softDelete(loanId.value)
    }
}
