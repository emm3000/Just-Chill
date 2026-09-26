package com.emm.justchill.feature.transaction.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.category.CategoryUi
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.transaction.TransactionUi
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class SeeTransactionsMonthSummaryLabelsTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun `reads Entro and Neto with a leading plus on a positive net`() {
        val day: LocalDate = LocalDate(2026, Month.MAY, 12)
        val movement = TransactionUi(
            transactionId = "movement-1",
            type = TransactionType.Spend,
            amount = "S/ 25",
            description = "Pan",
            occurredAt = LocalDateTime(2026, Month.MAY, 12, 9, 0),
            categoryName = "Comida",
            accountName = "Efectivo",
            category = CategoryUi(iconId = null, colorId = null),
        )
        val state = SeeTransactionsUiState(
            month = YearMonth(2026, Month.MAY),
            days = listOf(DayGroup(date = day, today = day, transactions = listOf(movement))),
            summary = MonthSummaryUi(income = Money(10_000L), spend = Money(2_500L)),
            movementCount = 1L,
        )

        composeRule.setContent {
            EmmTheme {
                SeeTransactionsContent(
                    state = state,
                    onIntent = {},
                    navigateToEdit = {},
                )
            }
        }

        composeRule.onNodeWithText("Entró", substring = true, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Neto +S/", substring = true, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `the eyebrow contentDescription names the month alone when it is the current year`() {
        val state = SeeTransactionsUiState(
            month = YearMonth(2026, Month.MAY),
            currentMonth = YearMonth(2026, Month.MAY),
            movementCount = 0L,
        )

        composeRule.setContent {
            EmmTheme {
                SeeTransactionsContent(state = state, onIntent = {}, navigateToEdit = {})
            }
        }

        composeRule
            .onNodeWithContentDescription("Gastado en Mayo. Cambiar de mes", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `the eyebrow contentDescription appends the year once it is not the current one`() {
        val state = SeeTransactionsUiState(
            month = YearMonth(2025, Month.MAY),
            currentMonth = YearMonth(2026, Month.MAY),
            movementCount = 0L,
        )

        composeRule.setContent {
            EmmTheme {
                SeeTransactionsContent(state = state, onIntent = {}, navigateToEdit = {})
            }
        }

        composeRule
            .onNodeWithContentDescription("Gastado en Mayo 2025. Cambiar de mes", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
