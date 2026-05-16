package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.error.DomainException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class UpdateTransactionUseCaseTest {

    private val repository = mockk<TransactionRepository>()
    private val dateAndTimeCombiner = mockk<DateAndTimeCombiner>()
    private val useCase = UpdateTransactionUseCase(repository, dateAndTimeCombiner)

    private val oldTransaction = Transaction.Empty.copy(transactionId = TransactionId("tx-1"))
    private val anyUpdate = TransactionUpdate(
        type = TransactionType.Spend,
        amount = 50.0,
        description = "updated",
        accountId = AccountId("acc-1"),
        categoryId = null,
        date = 2_000L,
    )

    @Test
    fun `update should call repository with combined date and original transactionId`() = runTest {
        every { dateAndTimeCombiner.combineWithUtc(2_000L) } returns 7_777L
        coEvery { repository.update(any(), any()) } just Runs

        useCase(oldTransaction, anyUpdate)

        coVerify(exactly = 1) {
            repository.update(
                transactionId = TransactionId("tx-1"),
                transactionUpdate = anyUpdate.copy(date = 7_777L),
            )
        }
    }

    @Test
    fun `update should keep other update fields intact`() = runTest {
        every { dateAndTimeCombiner.combineWithUtc(any()) } returns 0L
        coEvery { repository.update(any(), any()) } just Runs

        useCase(oldTransaction, anyUpdate)

        coVerify {
            repository.update(
                transactionId = any(),
                transactionUpdate = match {
                    it.type == anyUpdate.type &&
                        it.amount == anyUpdate.amount &&
                        it.description == anyUpdate.description &&
                        it.accountId == anyUpdate.accountId &&
                        it.categoryId == anyUpdate.categoryId
                },
            )
        }
    }

    @Test
    fun `update should propagate DomainException from repository`() = runTest {
        every { dateAndTimeCombiner.combineWithUtc(any()) } returns 0L
        coEvery { repository.update(any(), any()) } throws DomainException.NotFound("transaction")

        assertFailsWith<DomainException.NotFound> { useCase(oldTransaction, anyUpdate) }
    }
}
