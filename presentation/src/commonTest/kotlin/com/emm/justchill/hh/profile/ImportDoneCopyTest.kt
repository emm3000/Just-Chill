package com.emm.justchill.hh.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class ImportDoneCopyTest {

    @Test
    fun `leaves the original sentence untouched when no recurring movement landed`() {
        assertEquals(
            "Listo — 3 movimientos importados.",
            buildImportDoneMessage(transactions = 3, recurring = 0),
        )
    }

    @Test
    fun `uses the singular for a single recurring movement`() {
        assertEquals(
            "Listo — 3 movimientos y 1 recurrente importados.",
            buildImportDoneMessage(transactions = 3, recurring = 1),
        )
    }

    @Test
    fun `names both counts when more than one recurring movement landed`() {
        assertEquals(
            "Listo — 3 movimientos y 5 recurrentes importados.",
            buildImportDoneMessage(transactions = 3, recurring = 5),
        )
    }

    @Test
    fun `does not fix the pre-existing missing singular on the transactions side`() {
        // "1 movimientos importados" reads wrong, and it is out of scope for this change — see
        // the class KDoc. This test pins the current (imperfect) behaviour so nobody "fixes" it
        // as a drive-by inside an unrelated commit.
        assertEquals(
            "Listo — 1 movimientos importados.",
            buildImportDoneMessage(transactions = 1, recurring = 0),
        )
    }
}
