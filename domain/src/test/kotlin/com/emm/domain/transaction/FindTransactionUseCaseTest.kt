package com.emm.domain.transaction

import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.error.DomainException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FindTransactionUseCaseTest {

    private val repository = mockk<TransactionRepository>()
    private val useCase = FindTransactionUseCase(repository)

    @Test
    fun `find should return transaction when repository returns one`() = runTest {
        val transaction = Transaction.Empty.copy(transactionId = TransactionId("tx-1"))
        every { repository.find(TransactionId("tx-1")) } returns transaction

        val result = useCase(TransactionId("tx-1"))

        assertEquals(transaction, result)
        verify(exactly = 1) { repository.find(TransactionId("tx-1")) }
    }

    @Test
    fun `find should return null when repository returns null`() = runTest {
        every { repository.find(any()) } returns null

        assertNull(useCase(TransactionId("missing")))
    }

    @Test
    fun `find should propagate DomainException from repository`() = runTest {
        every { repository.find(any()) } throws DomainException.DatabaseError(RuntimeException("boom"))

        assertFailsWith<DomainException.DatabaseError> { useCase(TransactionId("tx-1")) }
    }
}
