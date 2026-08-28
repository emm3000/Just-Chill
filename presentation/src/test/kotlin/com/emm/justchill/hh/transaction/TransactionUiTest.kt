package com.emm.justchill.hh.transaction

import com.emm.domain.category.Category
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The two lines of a transaction row are decided here, never in the composable: the title is what
 * the user wrote, and the row falls back to what the app knows rather than to a placeholder.
 */
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

    @Test fun a_written_description_is_the_title() {
        assertEquals("Compra de la semana", row("Compra de la semana").title)
    }

    @Test fun a_blank_description_falls_back_to_the_category_name() {
        assertEquals("Supermercado", row("").title)
    }

    @Test fun a_movement_whose_category_is_gone_is_named_Sin_categoria() {
        assertEquals("Sin categoría", row("", category = null).categoryName)
        assertEquals("Sin categoría", row("", category = null).title)
    }

    @Test fun a_described_row_subtitles_with_the_category_and_the_account() {
        assertEquals("Supermercado · BCP", row("Compra de la semana").subtitle)
    }

    @Test fun a_row_titled_by_its_category_subtitles_with_the_account_alone() {
        assertEquals("BCP", row("").subtitle)
    }

    @Test fun an_account_the_join_could_not_name_leaves_the_category_alone() {
        assertEquals("Supermercado", row("Compra de la semana", accountName = "").subtitle)
    }

    @Test fun a_row_with_neither_a_description_nor_an_account_still_has_a_title() {
        val orphan = row("", category = null, accountName = "")

        assertEquals("Sin categoría", orphan.title)
        assertEquals("", orphan.subtitle)
    }
}
