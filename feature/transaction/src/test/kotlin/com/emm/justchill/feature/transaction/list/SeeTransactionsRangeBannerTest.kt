package com.emm.justchill.feature.transaction.list

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.ui.theme.EmmTheme
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class SeeTransactionsRangeBannerTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun `a range with no category renders its text and the clear intent fires`() {
        var cleared = false
        val state = SeeTransactionsUiState(
            month = YearMonth(2026, Month.AUGUST),
            movementCount = 3L,
            minAmount = Money(2_000L),
            maxAmount = Money(5_000L),
        )

        composeRule.setContent {
            EmmTheme {
                SeeTransactionsContent(
                    state = state,
                    onIntent = { intent ->
                        if (intent == SeeTransactionsIntent.OnClearCategoryFilter) cleared = true
                    },
                    navigateToEdit = {},
                    navigateToAdd = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("S/ 20.00 – S/ 50.00", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Limpiar filtro").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Limpiar filtro").performClick()

        assert(cleared) { "the clear affordance must send OnClearCategoryFilter" }
    }
}
