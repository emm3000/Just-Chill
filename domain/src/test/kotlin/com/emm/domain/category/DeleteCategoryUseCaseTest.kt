package com.emm.domain.category

import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class DeleteCategoryUseCaseTest {

    private val repository = mockk<CategoryRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val recurringMovementRepository = mockk<RecurringMovementRepository>()
    private val useCase = DeleteCategoryUseCase(
        repository,
        transactionRepository,
        recurringMovementRepository,
    )

    @Test
    fun `delete nulls categoryId on live transactions and then tombstones category`() = runTest {
        coEvery { transactionRepository.nullCategoryOnLiveRows(any()) } just Runs
        coEvery { recurringMovementRepository.nullCategoryOnLiveRows(any()) } just Runs
        coEvery { repository.delete(any()) } just Runs

        useCase(CategoryId("cat-1"))

        coVerify(exactly = 1) { transactionRepository.nullCategoryOnLiveRows(CategoryId("cat-1")) }
        coVerify(exactly = 1) { recurringMovementRepository.nullCategoryOnLiveRows(CategoryId("cat-1")) }
        coVerify(exactly = 1) { repository.delete(CategoryId("cat-1")) }
    }

    @Test
    fun `delete calls nullCategoryOnLiveRows before tombstoning category`() = runTest {
        val callOrder = mutableListOf<String>()
        coEvery { transactionRepository.nullCategoryOnLiveRows(any()) } answers { callOrder += "txn-null" }
        coEvery { recurringMovementRepository.nullCategoryOnLiveRows(any()) } answers { callOrder += "rm-null" }
        coEvery { repository.delete(any()) } answers { callOrder += "category-delete" }

        useCase(CategoryId("cat-2"))

        // Integrity must be enforced before the category is tombstoned.
        assert(callOrder.indexOf("category-delete") > callOrder.indexOf("txn-null"))
        assert(callOrder.indexOf("category-delete") > callOrder.indexOf("rm-null"))
    }

    @Test
    fun `delete should propagate DomainException from repository`() = runTest {
        coEvery { transactionRepository.nullCategoryOnLiveRows(any()) } just Runs
        coEvery { recurringMovementRepository.nullCategoryOnLiveRows(any()) } just Runs
        coEvery { repository.delete(any()) } throws DomainException.DatabaseError(RuntimeException("nope"))

        assertFailsWith<DomainException.DatabaseError> { useCase(CategoryId("cat-1")) }
    }
}
