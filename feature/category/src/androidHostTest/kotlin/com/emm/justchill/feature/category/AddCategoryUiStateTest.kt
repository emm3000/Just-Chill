package com.emm.justchill.feature.category

import kotlin.test.Test
import kotlin.test.assertEquals

class AddCategoryUiStateTest {

    @Test
    fun `a blank name asks for one on the save button`() {
        assertEquals("Escribe un nombre", AddCategoryUiState(name = "   ").saveLabel)
    }

    @Test
    fun `a typed name is quoted trimmed on the save button`() {
        assertEquals("Crear «Regalos»", AddCategoryUiState(name = "  Regalos ").saveLabel)
    }

    @Test
    fun `a blank name previews as a placeholder`() {
        val state: AddCategoryUiState = AddCategoryUiState(name = " ")

        assertEquals("Tu categoría" to true, state.previewName to state.isPreviewPlaceholder)
    }

    @Test
    fun `a typed name previews trimmed`() {
        val state: AddCategoryUiState = AddCategoryUiState(name = " Regalos ")

        assertEquals("Regalos" to false, state.previewName to state.isPreviewPlaceholder)
    }
}
