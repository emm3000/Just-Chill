package com.emm.domain.transaction

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.error.DomainException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SearchTransactionsUseCaseTest {

    private val repository = mockk<TransactionRepository>()
    private val useCase = SearchTransactionsUseCase(repository)

    @Test
    fun `empty filter delegates to repository with TransactionFilter None`() = runTest {
        every { repository.searchWithCategory(TransactionFilter.None) } returns flowOf(emptyList())

        val result = useCase(TransactionFilter.None).first()

        assertEquals(emptyList(), result)
        verify(exactly = 1) { repository.searchWithCategory(TransactionFilter.None) }
    }

    @Test
    fun `filter with query passes query to repository`() = runTest {
        val filter = TransactionFilter(query = "café")
        every { repository.searchWithCategory(filter) } returns flowOf(emptyList())

        useCase(filter).first()

        verify(exactly = 1) { repository.searchWithCategory(filter) }
    }

    @Test
    fun `filter with category ids passes category ids to repository`() = runTest {
        val filter = TransactionFilter(categoryIds = setOf(CategoryId("cat-1"), CategoryId("cat-2")))
        every { repository.searchWithCategory(filter) } returns flowOf(emptyList())

        useCase(filter).first()

        verify(exactly = 1) { repository.searchWithCategory(filter) }
    }

    @Test
    fun `errors from repository propagate to caller`() = runTest {
        val filter = TransactionFilter(query = "boom")
        val exception = DomainException.DatabaseError(RuntimeException("db error"))
        every { repository.searchWithCategory(filter) } returns flow { throw exception }

        assertFailsWith<DomainException.DatabaseError> {
            useCase(filter).first()
        }
    }
}
