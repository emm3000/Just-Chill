package com.emm.domain.transaction

import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.error.DomainException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class DeleteTransactionUseCaseTest {

    private val repository = mockk<TransactionRepository>()
    private val useCase = DeleteTransactionUseCase(repository)

    @Test
    fun `delete should call repository with given id`() = runTest {
        coEvery { repository.delete(any()) } just Runs

        useCase(TransactionId("tx-1"))

        coVerify(exactly = 1) { repository.delete(TransactionId("tx-1")) }
    }

    @Test
    fun `delete should propagate DomainException from repository`() = runTest {
        coEvery { repository.delete(any()) } throws DomainException.DatabaseError(RuntimeException("nope"))

        assertFailsWith<DomainException.DatabaseError> { useCase(TransactionId("tx-1")) }
    }
}
