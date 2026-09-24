package com.emm.justchill.feature.transaction.list

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class SeeTransactionsContentColumnTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    private val contentColumn: Dp = 24.dp

    private val roundingTolerance: Float = 0.5f

    private val amountLabel: String = "S/ 12"

    @Test
    fun `keeps the back glyph and the first row on the same content column`() {
        renderMonthWithOneMovement()

        val rootBoundsLeft: Dp = composeRule.onRoot().getUnclippedBoundsInRoot().left
        val rootBoundsRight: Dp = composeRule.onRoot().getUnclippedBoundsInRoot().right
        val backGlyphLeft: Dp = composeRule
            .onNodeWithContentDescription("Volver", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .left
        val rowAmountRight: Dp = composeRule
            .onNodeWithText(amountLabel, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .right

        assertEquals(contentColumn.value, (backGlyphLeft - rootBoundsLeft).value, roundingTolerance)
        assertEquals(contentColumn.value, (rootBoundsRight - rowAmountRight).value, roundingTolerance)
    }

    private fun renderMonthWithOneMovement() {
        val day: LocalDate = LocalDate(2026, Month.MAY, 12)
        val movement = TransactionUi(
            transactionId = "movement-1",
            type = TransactionType.Spend,
            amount = amountLabel,
            description = "Pan",
            occurredAt = LocalDateTime(2026, Month.MAY, 12, 9, 0),
            categoryName = "Comida",
            accountName = "Efectivo",
            category = CategoryUi(iconId = null, colorId = null),
        )
        val state = SeeTransactionsUiState(
            month = YearMonth(2026, Month.MAY),
            days = listOf(DayGroup(date = day, today = day, transactions = listOf(movement))),
            movementCount = 1L,
        )

        composeRule.setContent {
            EmmTheme {
                SeeTransactionsContent(
                    state = state,
                    onIntent = {},
                    navigateToEdit = {},
                    navigateToAdd = {},
                    onBack = {},
                )
            }
        }
    }
}
