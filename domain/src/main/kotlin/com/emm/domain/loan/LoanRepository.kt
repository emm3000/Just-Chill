package com.emm.domain.loan

import com.emm.domain.shared.LoanId
import kotlinx.coroutines.flow.Flow

interface LoanRepository {

    fun balancesByPerson(): Flow<List<PersonBalance>>

    fun loansWithBalance(personKey: String): Flow<List<LoanBalance>>

    fun byId(loanId: LoanId): Flow<Loan?>

    suspend fun create(loan: Loan)

    suspend fun update(loan: Loan)

    /** Must soft-delete the loan's live payments in the same call — the schema cannot enforce it. */
    suspend fun delete(loanId: LoanId)
}
