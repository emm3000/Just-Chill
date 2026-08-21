package com.emm.domain.loan

import com.emm.domain.shared.LoanId
import com.emm.domain.shared.error.DomainException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class DeleteLoanUseCaseTest {

    private val loanRepository = mockk<LoanRepository>()
    private val useCase = DeleteLoanUseCase(loanRepository)

    @Test
    fun `delete should call repository with given id`() = runTest {
        coEvery { loanRepository.delete(any()) } just Runs

        useCase(LoanId("loan-1"))

        coVerify(exactly = 1) { loanRepository.delete(LoanId("loan-1")) }
    }

    @Test
    fun `delete should propagate DomainException from repository`() = runTest {
        coEvery { loanRepository.delete(any()) } throws DomainException.DatabaseError(RuntimeException("nope"))

        assertFailsWith<DomainException.DatabaseError> { useCase(LoanId("loan-1")) }
    }
}
