package com.emm.justchill.feature.report

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.feature.report.components.IncomeByCategoryBars
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class IncomeByCategoryBarsTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun `announces the category name before its amount and share`() {
        val share: CategoryShare = CategoryShare(
            categoryId = "cat-1",
            name = "Sueldo",
            amountFormatted = "S/ 20.00",
            percentage = 40,
            colorKey = "green",
        )

        composeRule.setContent {
            EmmTheme {
                IncomeByCategoryBars(shares = listOf(share))
            }
        }

        composeRule
            .onNodeWithContentDescription("Sueldo: S/ 20.00, 40 por ciento del total")
            .assertExists()
    }
}
