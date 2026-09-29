package com.emm.justchill.core.presentation.category

import kotlin.test.Test
import kotlin.test.assertEquals

class IconCatalogTest {

    @Test
    fun `a blank query offers the whole catalog in its order`() {
        assertEquals(AppIconCatalog.catalog, AppIconCatalog.search(""))
    }

    @Test
    fun `search matches a keyword whatever its case`() {
        assertEquals(listOf("pizza"), AppIconCatalog.search("PIZZA").map(IconCatalog::id))
    }

    @Test
    fun `search matches part of a keyword`() {
        assertEquals(listOf("coffee"), AppIconCatalog.search("cafecit").map(IconCatalog::id))
    }

    @Test
    fun `search returns every icon sharing a keyword, in catalog order`() {
        assertEquals(listOf("mortgage", "loan"), AppIconCatalog.search("banco").map(IconCatalog::id))
    }

    @Test
    fun `search never matches the Spanish name alone`() {
        assertEquals(emptyList(), AppIconCatalog.search("pizzería"))
    }

    @Test
    fun `an unknown id falls back to the first icon`() {
        assertEquals("food", AppIconCatalog.findById("nope").id)
    }

    @Test
    fun `a known id finds its icon with its Spanish name`() {
        assertEquals("Transporte público", AppIconCatalog.findById("bus").label)
    }

    @Test
    fun `every id is unique`() {
        val ids: List<String> = AppIconCatalog.catalog.map(IconCatalog::id)

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `offers six colours in picker order, each with its Spanish name`() {
        assertEquals(
            listOf("Pizarra", "Salvia", "Terracota", "Malva", "Ocre", "Grafito"),
            selectableColorIds.map(::colorLabel),
        )
    }

    @Test
    fun `a colour id outside the picker is named by its id`() {
        assertEquals("teal", colorLabel("teal"))
    }
}
