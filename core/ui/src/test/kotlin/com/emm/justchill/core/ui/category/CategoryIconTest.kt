package com.emm.justchill.core.ui.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.emm.justchill.core.presentation.category.CategoryUi
import com.emm.justchill.core.presentation.category.IconCatalog
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryIconTest {

    @Test
    fun `every catalog icon draws a vector`() {
        val drawn: List<ImageVector> = IconCatalog.entries.map(IconCatalog::icon)

        assertEquals(IconCatalog.entries.size, drawn.size)
    }

    @Test
    fun `an icon draws the vector it always drew`() {
        assertEquals(
            listOf(Icons.Rounded.LocalPizza, Icons.Rounded.AccountBalance, Icons.Rounded.AccountBalance),
            listOf(IconCatalog.Pizza.icon, IconCatalog.Mortgage.icon, IconCatalog.Loan.icon),
        )
    }

    @Test
    fun `a stored category draws its icon, an unknown id the first icon, none a question mark`() {
        assertEquals(
            listOf(Icons.Rounded.LocalPizza, Icons.Rounded.Restaurant, Icons.Rounded.QuestionMark),
            listOf(
                CategoryUi(iconId = "pizza", colorId = null).resolvedIcon,
                CategoryUi(iconId = "nope", colorId = null).resolvedIcon,
                CategoryUi(iconId = null, colorId = null).resolvedIcon,
            ),
        )
    }
}
