package com.emm.justchill.feature.category

import com.emm.justchill.core.domain.category.Category
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteCategoryCopyTest {

    private val doomed: Category = category("c1", "Taxi")

    @Test
    fun `states how many movements lose their category`() {
        assertEquals(
            "47 movimientos van a quedar sin categoría. No puedes deshacerlo desde la app.",
            pendingDeleteMessage(affectedCount = 47),
        )
    }

    @Test
    fun `uses the singular for a single movement`() {
        assertEquals(
            "1 movimiento va a quedar sin categoría. No puedes deshacerlo desde la app.",
            pendingDeleteMessage(affectedCount = 1),
        )
    }

    @Test
    fun `says nothing is affected when the category is unused`() {
        // Without this branch the copy warned about consequences that did not exist, which is
        // how a harmless cleanup ends up feeling as risky as wiping the history.
        assertEquals(
            "Ningún movimiento la usa, así que no cambia nada de tu historial.",
            pendingDeleteMessage(affectedCount = 0),
        )
    }

    private fun pendingDeleteMessage(affectedCount: Int): String? = CategoriesUiState(
        categories = listOf(doomed),
        txCountByCategory = mapOf(doomed.categoryId to affectedCount),
        pendingDelete = doomed,
    ).pendingDeleteMessage
}
