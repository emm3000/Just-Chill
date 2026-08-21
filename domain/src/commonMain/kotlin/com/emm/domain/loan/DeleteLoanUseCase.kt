package com.emm.domain.loan

import com.emm.domain.shared.LoanId

class DeleteLoanUseCase(private val loanRepository: LoanRepository) {

    suspend operator fun invoke(loanId: LoanId) = loanRepository.delete(loanId)
}
