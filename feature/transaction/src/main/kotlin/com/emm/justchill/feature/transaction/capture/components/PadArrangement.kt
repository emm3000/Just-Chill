package com.emm.justchill.feature.transaction.capture.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

internal enum class PadArrangement {
    StackedTall,
    StackedShort,
}

internal val PadArrangement.isStackedTall: Boolean
    get() = this == PadArrangement.StackedTall

private enum class PadSlot {
    TallBudget,
    Pad,
}

// @Suppress: the TallBudget slot is a measure-only copy that is never placed, so its state is meant to be separate.
@Suppress("ContentSlotReused")
@Composable
internal fun PadArrangementLayout(
    heroFontSize: TextUnit,
    modifier: Modifier = Modifier,
    pad: @Composable (PadArrangement) -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints: Constraints ->
        val tallBudget: Placeable = subcompose(PadSlot.TallBudget) {
            Box(modifier = Modifier.clearAndSetSemantics {}) { pad(PadArrangement.StackedTall) }
        }.single().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val heroFloor: Dp = heroFontSize.toDp() * PLEX_MONO_LINE_BOX_EM
        val fitsTall: Boolean = tallBudget.height + heroFloor.roundToPx() <= constraints.maxHeight
        val arrangement: PadArrangement = if (fitsTall) PadArrangement.StackedTall else PadArrangement.StackedShort
        val placeable: Placeable = subcompose(PadSlot.Pad) { Box { pad(arrangement) } }.single().measure(constraints)

        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}

// IBM Plex Mono's ascent plus descent is 1.3em and the hero sets no lineHeight, so this is the hero's one-line box.
private const val PLEX_MONO_LINE_BOX_EM: Float = 1.3f
