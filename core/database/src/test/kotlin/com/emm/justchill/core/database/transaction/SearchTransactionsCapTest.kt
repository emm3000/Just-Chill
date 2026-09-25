package com.emm.justchill.core.database.transaction

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchTransactionsCapTest {

    private lateinit var fixture: TransactionSearchFixture

    @Before
    fun setUp() {
        fixture = TransactionSearchFixture()
    }

    @After
    fun tearDown() {
        fixture.close()
    }

    @Test
    fun `search returns at most 200 rows even when more match`() = runTest {
        repeat(205) { index ->
            insert(id = "t-$index", description = "pollo a la brasa $index", minutesFromStart = index)
        }

        val results: List<TransactionWithCategoryEntity> = search()

        assertEquals(200, results.size)
    }

    @Test
    fun `the cap keeps the newest matches`() = runTest {
        repeat(205) { index ->
            insert(id = "t-$index", description = "pollo a la brasa $index", minutesFromStart = index)
        }

        val results: List<TransactionWithCategoryEntity> = search()

        assertEquals(200, results.size)
        assertTrue(results.none { it.occurredAt < "2026-08-01T00:05:00" })
        assertTrue(results.any { it.occurredAt == "2026-08-04T00:24:00" }, "the newest row must survive")
    }

    @Test
    fun `fewer matches than the cap come back complete`() = runTest {
        repeat(3) { index ->
            insert(id = "t-$index", description = "pollo a la brasa $index", minutesFromStart = index)
        }

        val results: List<TransactionWithCategoryEntity> = search()

        assertEquals(3, results.size)
    }

    private suspend fun search(): List<TransactionWithCategoryEntity> = fixture.dataSource.searchTransactions(
        query = "pollo",
        categoryIds = emptySet(),
        minAmountCents = null,
        maxAmountCents = null,
    ).first()

    private fun insert(id: String, description: String, minutesFromStart: Int) {
        val occurredAt: String =
            "2026-08-%02dT00:%02d:00".format(Locale.ROOT, 1 + minutesFromStart / 60, minutesFromStart % 60)
        fixture.insertTransaction(id = id, description = description, occurredAt = occurredAt)
    }
}
