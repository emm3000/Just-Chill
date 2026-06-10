package com.emm.data.shared

import com.emm.domain.category.CategoryType
import com.emm.domain.recurring.Frequency
import com.emm.domain.transaction.TransactionType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnumParsingTest {

    // ── Known values parse correctly ──────────────────────────────────────────

    @Test
    fun `enumValueOrNull - known TransactionType Income returns Income`() {
        assertEquals(TransactionType.Income, enumValueOrNull<TransactionType>("Income"))
    }

    @Test
    fun `enumValueOrNull - known TransactionType Spend returns Spend`() {
        assertEquals(TransactionType.Spend, enumValueOrNull<TransactionType>("Spend"))
    }

    @Test
    fun `enumValueOrNull - known CategoryType Income returns Income`() {
        assertEquals(CategoryType.Income, enumValueOrNull<CategoryType>("Income"))
    }

    @Test
    fun `enumValueOrNull - known CategoryType Spend returns Spend`() {
        assertEquals(CategoryType.Spend, enumValueOrNull<CategoryType>("Spend"))
    }

    @Test
    fun `enumValueOrNull - known Frequency Monthly returns Monthly`() {
        assertEquals(Frequency.Monthly, enumValueOrNull<Frequency>("Monthly"))
    }

    // ── Unknown values return null ────────────────────────────────────────────

    @Test
    fun `enumValueOrNull - completely unknown value returns null`() {
        assertNull(enumValueOrNull<TransactionType>("Transfer"))
    }

    @Test
    fun `enumValueOrNull - empty string returns null`() {
        assertNull(enumValueOrNull<TransactionType>(""))
    }

    // ── Case-mismatch values return null (case-sensitive match) ───────────────

    @Test
    fun `enumValueOrNull - uppercase INCOME returns null`() {
        assertNull(enumValueOrNull<TransactionType>("INCOME"))
    }

    @Test
    fun `enumValueOrNull - uppercase SPEND returns null`() {
        assertNull(enumValueOrNull<TransactionType>("SPEND"))
    }

    @Test
    fun `enumValueOrNull - lowercase income returns null`() {
        assertNull(enumValueOrNull<TransactionType>("income"))
    }

    @Test
    fun `enumValueOrNull - mixed case iNcOmE returns null`() {
        assertNull(enumValueOrNull<TransactionType>("iNcOmE"))
    }

    @Test
    fun `enumValueOrNull - uppercase MONTHLY Frequency returns null`() {
        assertNull(enumValueOrNull<Frequency>("MONTHLY"))
    }
}
