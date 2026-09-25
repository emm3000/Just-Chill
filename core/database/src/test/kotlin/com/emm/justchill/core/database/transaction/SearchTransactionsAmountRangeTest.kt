package com.emm.justchill.core.database.transaction

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SearchTransactionsAmountRangeTest {

    private lateinit var fixture: TransactionSearchFixture

    @Before
    fun setUp() {
        fixture = TransactionSearchFixture()
        fixture.insertCategory("cat-1")
    }

    @After
    fun tearDown() {
        fixture.close()
    }

    private suspend fun search(
        query: String = "",
        categoryIds: Set<String> = emptySet(),
        minAmount: Long? = null,
        maxAmount: Long? = null,
    ): List<String> {
        val results: List<TransactionWithCategoryEntity> = fixture.dataSource.searchTransactions(
            query = query,
            categoryIds = categoryIds,
            minAmountCents = minAmount,
            maxAmountCents = maxAmount,
        ).first()
        return results.map { it.transactionId }
    }

    @Test
    fun `a row equal to the minimum bound comes back`() = runTest {
        fixture.insertTransaction(
            id = "t-min",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 2_000L,
        )
        fixture.insertTransaction(
            id = "t-below",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 1_999L,
        )

        val results: List<String> = search(minAmount = 2_000L)

        assertEquals(listOf("t-min"), results)
    }

    @Test
    fun `a row equal to the maximum bound comes back`() = runTest {
        fixture.insertTransaction(
            id = "t-max",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 5_000L,
        )
        fixture.insertTransaction(
            id = "t-above",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 5_001L,
        )

        val results: List<String> = search(maxAmount = 5_000L)

        assertEquals(listOf("t-max"), results)
    }

    @Test
    fun `one cent outside either bound does not come back`() = runTest {
        fixture.insertTransaction(
            id = "t-in",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 3_000L,
        )
        fixture.insertTransaction(
            id = "t-below",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 1_999L,
        )
        fixture.insertTransaction(
            id = "t-above",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 5_001L,
        )

        val results: List<String> = search(minAmount = 2_000L, maxAmount = 5_000L)

        assertEquals(listOf("t-in"), results)
    }

    @Test
    fun `min only excludes rows under it and keeps everything above`() = runTest {
        fixture.insertTransaction(
            id = "t-below",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 1_000L,
        )
        fixture.insertTransaction(
            id = "t-at",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 2_000L,
        )
        fixture.insertTransaction(
            id = "t-above",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 9_000L,
        )

        val results: List<String> = search(minAmount = 2_000L)

        assertEquals(setOf("t-at", "t-above"), results.toSet())
    }

    @Test
    fun `max only excludes rows over it and keeps everything below`() = runTest {
        fixture.insertTransaction(
            id = "t-below",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 1_000L,
        )
        fixture.insertTransaction(
            id = "t-at",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 5_000L,
        )
        fixture.insertTransaction(
            id = "t-above",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 9_000L,
        )

        val results: List<String> = search(maxAmount = 5_000L)

        assertEquals(setOf("t-below", "t-at"), results.toSet())
    }

    @Test
    fun `a range combines with a query and a category`() = runTest {
        fixture.insertTransaction(
            id = "t-match",
            description = "pollo a la brasa",
            occurredAt = "2026-08-01T00:00:00",
            amount = 3_000L,
            categoryId = "cat-1",
        )
        fixture.insertTransaction(
            id = "t-wrong-amount",
            description = "pollo a la brasa",
            occurredAt = "2026-08-01T00:00:00",
            amount = 9_000L,
            categoryId = "cat-1",
        )
        fixture.insertTransaction(
            id = "t-wrong-category",
            description = "pollo a la brasa",
            occurredAt = "2026-08-01T00:00:00",
            amount = 3_000L,
        )

        val results: List<String> = search(
            query = "pollo",
            categoryIds = setOf("cat-1"),
            minAmount = 2_000L,
            maxAmount = 5_000L,
        )

        assertEquals(listOf("t-match"), results)
    }

    @Test
    fun `a soft-deleted row in range stays out`() = runTest {
        fixture.insertTransaction(
            id = "t-live",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 3_000L,
        )
        fixture.insertTransaction(
            id = "t-deleted",
            description = "desc",
            occurredAt = "2026-08-01T00:00:00",
            amount = 3_000L,
            deletedAt = 2L,
        )

        val results: List<String> = search(minAmount = 2_000L, maxAmount = 5_000L)

        assertEquals(listOf("t-live"), results)
    }
}
