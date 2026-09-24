package com.emm.justchill.feature.transaction.capture.components

import androidx.compose.ui.unit.Dp
import com.emm.justchill.core.ui.theme.WrappedCombosMinHeight

internal enum class PadArrangement {
    StackedTall,
    StackedShort,
}

internal val PadArrangement.isStackedTall: Boolean
    get() = this == PadArrangement.StackedTall

internal fun padArrangement(maxHeight: Dp): PadArrangement =
    if (maxHeight >= WrappedCombosMinHeight) PadArrangement.StackedTall else PadArrangement.StackedShort
