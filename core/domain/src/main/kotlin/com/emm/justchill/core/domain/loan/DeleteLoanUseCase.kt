package com.emm.justchill.core.domain.loan

import com.emm.justchill.core.domain.shared.LoanId

class DeleteLoanUseCase(private val loanRepository: LoanRepository) {

    suspend operator fun invoke(loanId: LoanId) = loanRepository.delete(loanId)
}
