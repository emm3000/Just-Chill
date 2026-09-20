package com.emm.justchill.feature.report

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.ui.theme.EmmTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReportTopBarTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    private val shareGlyphEndGap: Dp = 28.dp

    private val roundingTolerance: Float = 0.5f

    @Test
    fun `keeps the share glyph on the content column at the top bar end`() {
        renderReport()

        val rootEnd: Dp = composeRule.onRoot().getUnclippedBoundsInRoot().right
        val glyphEnd: Dp = composeRule
            .onNodeWithContentDescription("Compartir reporte", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .right

        assertEquals(shareGlyphEndGap.value, (rootEnd - glyphEnd).value, roundingTolerance)
    }

    private fun renderReport() {
        composeRule.setContent {
            EmmTheme {
                ReportScreen(
                    state = ReportUiState(
                        month = YearMonth(2026, Month.MAY),
                        isMonthEmpty = true,
                    ),
                    onAddTransaction = {},
                    onIntent = {},
                    onBack = {},
                )
            }
        }
    }
}
