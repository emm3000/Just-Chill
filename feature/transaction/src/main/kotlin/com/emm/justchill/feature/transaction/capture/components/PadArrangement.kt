package com.emm.justchill.feature.transaction.capture.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

@Composable
internal fun PadArrangementLayout(
    heroFontSize: TextUnit,
    tallBudget: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    pad: @Composable (PadArrangement) -> Unit,
) {
    val hiddenTallBudget: @Composable () -> Unit = remember(tallBudget) {
        { Box(modifier = Modifier.clearAndSetSemantics {}) { tallBudget() } }
    }
    val padByArrangement: Map<PadArrangement, @Composable () -> Unit> = remember(pad) {
        PadArrangement.entries.associateWith { arrangement: PadArrangement -> @Composable { pad(arrangement) } }
    }

    SubcomposeLayout(modifier = modifier) { constraints: Constraints ->
        val budget: Placeable = subcompose(PadSlot.TallBudget, hiddenTallBudget)
            .single()
            .measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val heroFloor: Dp = heroFontSize.toDp() * PLEX_MONO_LINE_BOX_EM
        val fitsTall: Boolean = budget.height + heroFloor.roundToPx() <= constraints.maxHeight
        val arrangement: PadArrangement = if (fitsTall) PadArrangement.StackedTall else PadArrangement.StackedShort
        val placeable: Placeable = subcompose(PadSlot.Pad, padByArrangement.getValue(arrangement))
            .single()
            .measure(constraints)

        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}

// IBM Plex Mono's ascent plus descent is 1.3em and the hero sets no lineHeight, so this is the hero's one-line box.
internal const val PLEX_MONO_LINE_BOX_EM: Float = 1.3f
