package com.emm.justchill.feature.transaction.capture.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.WrappedCombosMinHeight

internal enum class PadArrangement {
    StackedTall,
    StackedShort,
}

internal val PadArrangement.isStackedTall: Boolean
    get() = this == PadArrangement.StackedTall

@Composable
internal fun CapturePadLayout(
    menu: @Composable () -> Unit,
    monthLine: @Composable (Modifier) -> Unit,
    hero: @Composable (Modifier) -> Unit,
    form: @Composable (PadArrangement) -> Unit,
    numpad: @Composable (PadArrangement) -> Unit,
    cta: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    BoxWithConstraints(modifier = modifier) {
        val arrangement: PadArrangement =
            if (maxHeight >= WrappedCombosMinHeight) PadArrangement.StackedTall else PadArrangement.StackedShort

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(start = spacing.s4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                menu()
                monthLine(Modifier.weight(1f))
            }
            hero(Modifier.weight(1f).fillMaxWidth())
            form(arrangement)
            numpad(arrangement)
            cta()
        }
    }
}
