package com.emm.justchill.feature.loan

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.emm.justchill.core.ui.theme.EmmTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoanDetailScreenTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    private val recordedIntents: MutableList<LoanDetailIntent> = mutableListOf()

    private val openLoan: LoanSummaryUi = LoanSummaryUi(
        personName = "María",
        principal = "S/ 100.00",
        interestPercentLabel = "0%",
        totalDue = "S/ 100.00",
        paidSoFar = "S/ 0.00",
        remaining = "S/ 100.00",
        remainingCents = 10_000L,
        readableLentAt = "1 de agosto de 2026",
        note = "",
    )

    private val settledLoan: LoanSummaryUi = LoanSummaryUi(
        personName = "María",
        principal = "S/ 100.00",
        interestPercentLabel = "0%",
        totalDue = "S/ 100.00",
        paidSoFar = "S/ 100.00",
        remaining = "S/ 0.00",
        remainingCents = 0L,
        readableLentAt = "1 de agosto de 2026",
        note = "",
    )

    @Test
    fun `renders both loan actions in the top bar with a click action`() {
        renderLoanDetail(summary = openLoan)

        composeRule.onNodeWithContentDescription("Editar préstamo").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Eliminar préstamo").assertHasClickAction()
    }

    @Test
    fun `labels the payments section with the ABONOS eyebrow`() {
        renderLoanDetail(summary = openLoan)

        composeRule.onNodeWithText("ABONOS").assertExists()
    }

    @Test
    fun `renders the payment call to action with a click action while the loan is open`() {
        renderLoanDetail(summary = openLoan)

        composeRule.onNodeWithText("Registrar abono").assertHasClickAction()
    }

    @Test
    fun `clicking the payment call to action records the add payment intent`() {
        renderLoanDetail(summary = openLoan)

        composeRule.onNodeWithText("Registrar abono").performClick()

        assertEquals(LoanDetailIntent.PaymentFormIntent.OnAddPaymentClick, recordedIntents.single())
    }

    @Test
    fun `clicking the edit action records the edit loan intent`() {
        renderLoanDetail(summary = openLoan)

        composeRule.onNodeWithContentDescription("Editar préstamo").performClick()

        assertEquals(LoanDetailIntent.OnEditLoanClick, recordedIntents.single())
    }

    @Test
    fun `clicking the delete action records the delete loan intent`() {
        renderLoanDetail(summary = openLoan)

        composeRule.onNodeWithContentDescription("Eliminar préstamo").performClick()

        assertEquals(LoanDetailIntent.OnDeleteLoanClick, recordedIntents.single())
    }

    @Test
    fun `a settled loan keeps the payment call to action announced as a disabled button`() {
        renderLoanDetail(summary = settledLoan)

        composeRule.onNodeWithText("Registrar abono")
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    private fun renderLoanDetail(summary: LoanSummaryUi) {
        composeRule.setContent {
            EmmTheme {
                LoanDetailScreen(
                    state = LoanDetailUiState(summary = summary),
                    onIntent = { intent -> recordedIntents.add(intent) },
                    onBack = {},
                )
            }
        }
    }
}
