package com.emm.justchill.feature.transaction.capture

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import com.emm.justchill.core.domain.transaction.TransactionType
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1000dp-h1000dp")
class CapturePadArrangementStabilityTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun `toggling the sign keeps the arrangement while the combos go from none to three`() {
        val spend: AddTransactionUiState = populatedCaptureState()
        val incomeWithoutCombos: AddTransactionUiState = spend.copy(transactionType = TransactionType.Income)
        val shown: MutableState<AddTransactionUiState> = mutableStateOf(incomeWithoutCombos)
        composeRule.showPad(width = 360, height = 640, fontScale = 1f, state = { shown.value })

        val withoutCombos: PadShape = composeRule.padShape()
        shown.value = spend
        val withThreeCombos: PadShape = composeRule.padShape()

        assertEquals(0, incomeWithoutCombos.frequentCombos.size)
        assertEquals(3, spend.frequentCombos.size)
        assertEquals(withoutCombos, withThreeCombos)
    }

    private data class PadShape(val keyHeight: Dp, val hasEyebrow: Boolean)

    private fun ComposeContentTestRule.padShape(): PadShape {
        waitForIdle()
        val key: DpRect = onNodeWithText("1").getBoundsInRoot()
        val hasEyebrow: Boolean = onAllNodesWithText("TUS COMBINACIONES FRECUENTES").fetchSemanticsNodes().isNotEmpty()
        return PadShape(keyHeight = key.bottom - key.top, hasEyebrow = hasEyebrow)
    }
}
