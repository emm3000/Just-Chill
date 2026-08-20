package com.emm.justchill.hh.category

import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteCategoryCopyTest {

    @Test
    fun `states how many movements lose their category`() {
        assertEquals(
            "47 movimientos van a quedar sin categoría. No puedes deshacerlo desde la app.",
            buildDeleteCategoryMessage(affectedCount = 47),
        )
    }

    @Test
    fun `uses the singular for a single movement`() {
        assertEquals(
            "1 movimiento va a quedar sin categoría. No puedes deshacerlo desde la app.",
            buildDeleteCategoryMessage(affectedCount = 1),
        )
    }

    @Test
    fun `says nothing is affected when the category is unused`() {
        // Without this branch the copy warned about consequences that did not exist, which is
        // how a harmless cleanup ends up feeling as risky as wiping the history.
        assertEquals(
            "Ningún movimiento la usa, así que no cambia nada de tu historial.",
            buildDeleteCategoryMessage(affectedCount = 0),
        )
    }
}
