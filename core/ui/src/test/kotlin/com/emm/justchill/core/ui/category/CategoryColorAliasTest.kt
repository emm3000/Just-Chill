package com.emm.justchill.core.ui.category

import androidx.compose.ui.graphics.Color
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.emmDarkColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryColorAliasTest {

    private val colors: EmmColors = emmDarkColors

    private val categoryTokens: Set<Color> = setOf(
        colors.catSlate,
        colors.catSage,
        colors.catTerracotta,
        colors.catMauve,
        colors.catOchre,
        colors.catGraphite,
    )

    @Test
    fun `offers one id per category token`() {
        assertEquals(categoryTokens.size, selectableColorIds.size)
        assertEquals(categoryTokens, selectableColorIds.map(colors::resolvedColor).toSet())
    }

    @Test
    fun `resolves every colour id a stored category can carry`() {
        val storedIds: List<String> = listOf(
            "blue",
            "green",
            "red",
            "purple",
            "orange",
            "gray",
            "yellow",
            "teal",
            "pink",
            "brown",
        )
        storedIds.forEach { id: String ->
            assertTrue(colors.resolvedColor(id) in categoryTokens, id)
        }
    }

    @Test
    fun `resolves the legacy ids to the token they alias`() {
        assertEquals(colors.catOchre, colors.resolvedColor("yellow"))
        assertEquals(colors.catSage, colors.resolvedColor("teal"))
        assertEquals(colors.catMauve, colors.resolvedColor("pink"))
        assertEquals(colors.catTerracotta, colors.resolvedColor("brown"))
    }

    @Test
    fun `falls back to graphite for an unknown or missing id`() {
        assertEquals(colors.catGraphite, colors.resolvedColor("chartreuse"))
        assertEquals(colors.catGraphite, colors.resolvedColor(null))
    }
}
