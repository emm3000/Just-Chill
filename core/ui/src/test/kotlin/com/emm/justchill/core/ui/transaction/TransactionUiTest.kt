package com.emm.justchill.core.ui.transaction

import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.TransactionId
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.domain.transaction.TransactionWithCategory
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// The two lines of a transaction row are decided here, never in the composable: the title is what
// the user wrote, and the row falls back to what the app knows rather than to a placeholder.
class TransactionUiTest {

    private val spendCategory = Category(
        categoryId = CategoryId("cat-1"),
        name = "Supermercado",
        icon = "cart",
        color = "green",
        categoryType = CategoryType.Spend,
    )

    private fun row(
        description: String,
        category: Category? = spendCategory,
        accountName: String = "BCP",
    ): TransactionUi = listOf(
        TransactionWithCategory(
            transactionId = TransactionId("tx-1"),
            type = TransactionType.Spend,
            amount = Money(4250L),
            description = description,
            occurredAt = LocalDateTime(2026, 8, 10, 14, 30),
            accountId = AccountId("acc-1"),
            accountName = accountName,
            category = category,
        ),
    ).toUi().single()

    @Test
    fun `a written description is the title`() {
        assertEquals("Compra de la semana", row("Compra de la semana").title)
    }

    @Test
    fun `a blank description falls back to the category name`() {
        assertEquals("Supermercado", row("").title)
    }

    @Test
    fun `a movement whose category is gone is named Sin categoria`() {
        assertEquals("Sin categoría", row("", category = null).categoryName)
        assertEquals("Sin categoría", row("", category = null).title)
    }

    @Test
    fun `the category dot rides the title only when the title is the category name`() {
        assertTrue(row("").categoryLeadsTitle)
        assertFalse(row("Compra de la semana").categoryLeadsTitle)
    }

    @Test
    fun `a described row subtitles with the category, the account and the time`() {
        assertEquals("Supermercado · BCP · 14:30", row("Compra de la semana").subtitle)
    }

    @Test
    fun `a row titled by its category subtitles with the account and the time`() {
        assertEquals("BCP · 14:30", row("").subtitle)
    }

    @Test
    fun `an account the join could not name leaves the category and the time`() {
        assertEquals("Supermercado · 14:30", row("Compra de la semana", accountName = "").subtitle)
    }

    @Test
    fun `a row with neither a description nor an account still subtitles with the time`() {
        val orphan: TransactionUi = row("", category = null, accountName = "")

        assertEquals("Sin categoría", orphan.title)
        assertEquals("14:30", orphan.subtitle)
    }
}
