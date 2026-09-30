package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.presentation.category.SelectableCategory
import org.junit.Test
import kotlin.test.assertEquals

class CaptureSectionsTest {

    private val categories: List<SelectableCategory> = listOf(market, taxi, coffee)

    @Test
    fun `two frequent categories lead in rank order and the rest keep catalog order`() {
        val frequent: List<SelectableCategory> = frequentSectionOf(categories, listOf("coffee", "market"))

        assertEquals(listOf(coffee, market), frequent)
        assertEquals(listOf(taxi), otherSectionOf(categories, frequent))
    }

    @Test
    fun `a single frequent category makes no section and every category stays in the rest`() {
        val frequent: List<SelectableCategory> = frequentSectionOf(categories, listOf("coffee"))

        assertEquals(emptyList<SelectableCategory>(), frequent)
        assertEquals(categories, otherSectionOf(categories, frequent))
    }

    @Test
    fun `a frequent id naming a deleted category is skipped before the section is counted`() {
        val frequent: List<SelectableCategory> = frequentSectionOf(categories, listOf("gone", "taxi"))

        assertEquals(emptyList<SelectableCategory>(), frequent)
        assertEquals(categories, otherSectionOf(categories, frequent))
    }

    @Test
    fun `a search ignores accents, case and stray spaces`() {
        assertEquals(listOf(coffee), categories.matching("cafe "))
        assertEquals(listOf(market), categories.matching("  SUPER "))
    }

    @Test
    fun `a blank search matches every category in catalog order`() {
        assertEquals(categories, categories.matching("   "))
    }
}
