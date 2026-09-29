package com.emm.justchill.core.database.transaction

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SearchTransactionsAmountQueryTest {

    private lateinit var fixture: TransactionSearchFixture

    @Before
    fun setUp() {
        fixture = TransactionSearchFixture()
        fixture.insertCategory("cat-1")
        fixture.insertTransaction(
            id = "t-sueldo",
            description = "Sueldo",
            occurredAt = "2026-08-01T00:00:00",
            amount = 320_000L,
        )
        fixture.insertTransaction(
            id = "t-almuerzo",
            description = "Almuerzo",
            occurredAt = "2026-08-01T00:00:00",
            amount = 20_000L,
        )
        fixture.insertTransaction(
            id = "t-pago",
            description = "Pago 3200",
            occurredAt = "2026-08-01T00:00:00",
            amount = 100L,
        )
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
    ): Set<String> {
        val results: List<TransactionWithCategoryEntity> = fixture.dataSource.searchTransactions(
            query = query,
            categoryIds = categoryIds,
            minAmountCents = minAmount,
            maxAmountCents = maxAmount,
        ).first()
        return results.map { it.transactionId }.toSet()
    }

    @Test
    fun `plain digits match a row whose amount equals it`() = runTest {
        assertEquals(setOf("t-sueldo", "t-pago"), search(query = "3200"))
    }

    @Test
    fun `comma group separators still match by amount`() = runTest {
        assertEquals(setOf("t-sueldo", "t-pago"), search(query = "3,200"))
    }

    @Test
    fun `two decimals still match by amount`() = runTest {
        assertEquals(setOf("t-sueldo", "t-pago"), search(query = "3200.00"))
    }

    @Test
    fun `a digit substring never matches, only the exact amount`() = runTest {
        assertEquals(setOf("t-almuerzo", "t-pago"), search(query = "200"))
    }

    @Test
    fun `text still matches by description`() = runTest {
        assertEquals(setOf("t-sueldo"), search(query = "sueldo"))
    }

    @Test
    fun `a soft-deleted row with a matching amount stays out`() = runTest {
        fixture.insertTransaction(
            id = "t-deleted",
            description = "Borrado",
            occurredAt = "2026-08-01T00:00:00",
            amount = 320_000L,
            deletedAt = 2L,
        )

        assertEquals(setOf("t-sueldo", "t-pago"), search(query = "3200"))
    }

    @Test
    fun `an amount query combines with a category and a range`() = runTest {
        fixture.insertTransaction(
            id = "t-wrong-category",
            description = "Pago 3200",
            occurredAt = "2026-08-01T00:00:00",
            amount = 320_000L,
        )
        fixture.insertTransaction(
            id = "t-right-category",
            description = "Pago 3200",
            occurredAt = "2026-08-01T00:00:00",
            amount = 320_000L,
            categoryId = "cat-1",
        )

        val results: Set<String> = search(
            query = "3200",
            categoryIds = setOf("cat-1"),
            minAmount = 300_000L,
            maxAmount = 400_000L,
        )

        assertEquals(setOf("t-right-category"), results)
    }
}
