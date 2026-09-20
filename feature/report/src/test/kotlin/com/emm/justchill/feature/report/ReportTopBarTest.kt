package com.emm.justchill.feature.report

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.ui.theme.EmmTheme
import kotlin.test.Test
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReportTopBarTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    private val shareGlyphSize: Dp = 20.dp

    private val shareGlyphLeftEdge: Dp = 363.dp

    @Test
    fun `keeps the share glyph on the content column at the top bar end`() {
        renderReport()

        composeRule.onNodeWithContentDescription("Compartir reporte", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(shareGlyphLeftEdge)
            .assertWidthIsEqualTo(shareGlyphSize)
    }

    @Test
    fun `paints the share glyph at its catalog size`() {
        renderReport()

        composeRule.onNodeWithContentDescription("Compartir reporte", useUnmergedTree = true)
            .assertHeightIsEqualTo(shareGlyphSize)
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
                )
            }
        }
    }
}
