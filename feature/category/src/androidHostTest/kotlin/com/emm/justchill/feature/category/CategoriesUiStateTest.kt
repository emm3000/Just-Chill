package com.emm.justchill.feature.category

import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.CategoryId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CategoriesUiStateTest {

    private val transport: Category = category("c1", "Transporte")
    private val food: Category = category("c2", "comida")
    private val salary: Category = category("c3", "Sueldo", CategoryType.Income)
    private val bonus: Category = category("c4", "Bonos", CategoryType.Income)

    private val state: CategoriesUiState = CategoriesUiState(
        categories = listOf(transport, food, salary, bonus),
        txCountByCategory = mapOf(CategoryId("c1") to 5, CategoryId("c3") to 1),
        uncategorizedSpendCount = 4,
    )

    @Test
    fun `spend rows run alphabetically whatever the case, each with its movement count`() {
        assertEquals(
            listOf("c2" to "0 mov.", "c1" to "5 mov."),
            state.spendRows.map { row: CategoryRowUi -> row.id to row.movementCountLabel },
        )
    }

    @Test
    fun `income rows hold only income categories, alphabetically`() {
        assertEquals(listOf("Bonos", "Sueldo"), state.incomeRows.map(CategoryRowUi::name))
    }

    @Test
    fun `a row keys on its category id as a plain string, whatever the category is renamed to`() {
        val renamed: CategoriesUiState = state.copy(categories = listOf(transport.copy(name = "Taxis")))

        assertEquals(listOf("c1"), renamed.spendRows.map(CategoryRowUi::id))
    }

    @Test
    fun `a row carries its category's icon and colour ids and the category itself`() {
        val row: CategoryRowUi = state.spendRows.last()

        assertEquals(Triple("taxi", "green", transport), Triple(row.iconId, row.colorId, row.category))
    }

    @Test
    fun `the spend section counts the uncategorized row`() {
        assertEquals(3, state.spendSectionCount)
    }

    @Test
    fun `the uncategorized row phrases its spend count`() {
        assertEquals("4 mov.", state.uncategorizedCountLabel)
    }

    @Test
    fun `nothing pending delete has no delete message`() {
        assertNull(state.pendingDeleteMessage)
    }
}
