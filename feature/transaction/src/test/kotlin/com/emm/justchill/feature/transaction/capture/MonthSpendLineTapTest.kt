package com.emm.justchill.feature.transaction.capture

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.emm.justchill.core.ui.theme.EmmTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class MonthSpendLineTapTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun `a tap on the month total opens the movements once`() {
        var opened = 0

        composeRule.setContent {
            EmmTheme {
                AddTransactionScreenContent(
                    state = AddTransactionUiState(today = LocalDate(2026, Month.AUGUST, 10)),
                    onIntent = {},
                    onOpenMenu = {},
                    onOpenTransactions = { opened += 1 },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Gastado en Agosto: S/ 0.00. Toca para ver tus movimientos.")
            .performClick()

        assertEquals(1, opened)
    }
}
