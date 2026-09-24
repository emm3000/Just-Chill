package com.emm.justchill.feature.transaction.capture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.then
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmType

internal const val PAD_FRAME_TAG: String = "frame"

internal val REDMI_INSIDE_BARS_WINDOWS: List<Pair<Int, Int>> = listOf(
    360 to 728,
    360 to 720,
)

internal val PAD_WINDOWS: List<Pair<Int, Int>> = REDMI_INSIDE_BARS_WINDOWS + listOf(
    360 to 640,
    360 to 568,
    320 to 640,
)

internal fun ComposeContentTestRule.showPad(
    width: Int,
    height: Int,
    fontScale: Float,
    state: () -> AddTransactionUiState = { populatedCaptureState() },
    onAmountHero: (TextStyle, Density) -> Unit = { _, _ -> },
) {
    setContent {
        DeviceConfigurationOverride(
            DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, height.dp)) then
                DeviceConfigurationOverride.FontScale(fontScale),
        ) {
            EmmTheme {
                onAmountHero(LocalEmmType.current.amountHero, LocalDensity.current)
                Box(modifier = Modifier.fillMaxSize().testTag(PAD_FRAME_TAG)) {
                    AddTransactionScreenContent(
                        state = state(),
                        onIntent = {},
                        onOpenMenu = {},
                        onOpenTransactions = {},
                        onSave = {},
                    )
                }
            }
        }
    }
}
