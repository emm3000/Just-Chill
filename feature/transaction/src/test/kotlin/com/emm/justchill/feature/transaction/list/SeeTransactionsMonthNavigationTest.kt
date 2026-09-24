package com.emm.justchill.feature.transaction.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.ui.theme.EmmTheme
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class SeeTransactionsMonthNavigationTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun `keeps the month chevrons on a month with no movements`() {
        composeRule.setContent {
            EmmTheme {
                SeeTransactionsContent(
                    state = SeeTransactionsUiState(
                        month = YearMonth(2026, Month.NOVEMBER),
                        movementCount = 12L,
                    ),
                    onIntent = {},
                    navigateToEdit = {},
                    navigateToAdd = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Mes anterior", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Mes siguiente", useUnmergedTree = true).assertIsDisplayed()
    }
}
