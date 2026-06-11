package com.emm.justchill.core.ui.atoms

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightQuotedTest {

    @Test
    fun `no markers returns plain text without spans`() {
        val result = highlightQuoted("Hello world")
        assertEquals("Hello world", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `one pair bolds inner text without removing guillemets`() {
        val result = highlightQuoted("Categoría «Regalos» creada")
        assertEquals("Categoría «Regalos» creada", result.text)
        assertEquals(1, result.spanStyles.size)
        val span = result.spanStyles[0]
        // Inner text "Regalos" starts after «  at index 10+1=11, ends before » at 17
        val inner = result.text.substring(span.start, span.end)
        assertEquals("Regalos", inner)
        assertEquals(FontWeight.W700, span.item.fontWeight)
    }

    @Test
    fun `unclosed open guillemet is rendered literally with no bold span`() {
        val result = highlightQuoted("Categoría «sin cerrar")
        assertEquals("Categoría «sin cerrar", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `multiple pairs are each independently bolded`() {
        val result = highlightQuoted("«Uno» y «Dos»")
        assertEquals("«Uno» y «Dos»", result.text)
        assertEquals(2, result.spanStyles.size)

        val firstInner = result.text.substring(result.spanStyles[0].start, result.spanStyles[0].end)
        val secondInner = result.text.substring(result.spanStyles[1].start, result.spanStyles[1].end)
        assertEquals("Uno", firstInner)
        assertEquals("Dos", secondInner)
        result.spanStyles.forEach { span ->
            assertEquals(FontWeight.W700, span.item.fontWeight)
        }
    }

    @Test
    fun `empty pair produces a bold span over zero characters`() {
        val result = highlightQuoted("Vacío: «»")
        assertEquals("Vacío: «»", result.text)
        assertEquals(1, result.spanStyles.size)
        val span = result.spanStyles[0]
        assertEquals(span.start, span.end) // zero-width bold span
    }
}
