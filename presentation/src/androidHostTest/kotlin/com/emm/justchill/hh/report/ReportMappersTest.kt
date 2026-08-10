package com.emm.justchill.hh.report

import com.emm.domain.report.CategoryAmount
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.justchill.hh.shared.UiStrings
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReportMappersTest {

    @Test
    fun `uncategorized bucket gets the Sin categoria label and no color key`() {
        val share = uncategorized(cents = 500).toCategoryShare(percentage = 25)

        assertNull(share.categoryId)
        assertEquals(UiStrings.UNCATEGORIZED, share.name)
        // The neutral-graphite fallthrough happens at render time (domainColorToUi in :shared-ui).
        assertNull(share.colorKey)
    }

    @Test
    fun `a real category keeps its own name and raw color key`() {
        val share = categorized(id = "cat-1", name = "Comida", color = "green", cents = 1_000)
            .toCategoryShare(percentage = 50)

        assertEquals("cat-1", share.categoryId)
        assertEquals("Comida", share.name)
        assertEquals("green", share.colorKey)
    }

    @Test
    fun `shares percentages are computed over the total including the uncategorized bucket`() {
        val amounts = listOf(
            categorized(id = "cat-1", name = "Comida", color = "green", cents = 750),
            uncategorized(cents = 250),
        )

        val shares = buildShares(amounts, total = Money(1_000))

        assertEquals(listOf(75, 25), shares.map { it.percentage })
        assertEquals(listOf("Comida", UiStrings.UNCATEGORIZED), shares.map { it.name })
    }

    @Test
    fun `a zero total yields zero percent instead of dividing by zero`() {
        val shares = buildShares(listOf(uncategorized(cents = 0)), total = Money.Zero)

        assertEquals(listOf(0), shares.map { it.percentage })
    }

    private fun uncategorized(cents: Long) = CategoryAmount(
        categoryId = null,
        categoryName = null,
        categoryIcon = null,
        categoryColor = null,
        amount = Money(cents),
    )

    private fun categorized(id: String, name: String, color: String, cents: Long) = CategoryAmount(
        categoryId = CategoryId(id),
        categoryName = name,
        categoryIcon = "food",
        categoryColor = color,
        amount = Money(cents),
    )
}
