package com.emm.justchill.feature.transaction.capture.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

internal enum class PadArrangement {
    Stacked,
    SideBySide,
}

@Composable
internal fun CapturePadLayout(
    menu: @Composable () -> Unit,
    monthLine: @Composable (Modifier) -> Unit,
    hero: @Composable (Modifier) -> Unit,
    form: @Composable (PadArrangement) -> Unit,
    numpad: @Composable (PadArrangement, Modifier) -> Unit,
    cta: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val currentMenu: @Composable () -> Unit by rememberUpdatedState(menu)
    val currentMonthLine: @Composable (Modifier) -> Unit by rememberUpdatedState(monthLine)
    val currentHero: @Composable (Modifier) -> Unit by rememberUpdatedState(hero)
    val currentForm: @Composable (PadArrangement) -> Unit by rememberUpdatedState(form)
    val currentNumpad: @Composable (PadArrangement, Modifier) -> Unit by rememberUpdatedState(numpad)
    val movableMenu: @Composable () -> Unit = remember { movableContentOf { currentMenu() } }
    val movableMonthLine: @Composable (Modifier) -> Unit = remember {
        movableContentOf { lineModifier: Modifier -> currentMonthLine(lineModifier) }
    }
    val movableHero: @Composable (Modifier) -> Unit = remember {
        movableContentOf { heroModifier: Modifier -> currentHero(heroModifier) }
    }
    val movableForm: @Composable (PadArrangement) -> Unit = remember {
        movableContentOf { arrangement: PadArrangement -> currentForm(arrangement) }
    }
    val movableNumpad: @Composable (PadArrangement, Modifier) -> Unit = remember {
        movableContentOf { arrangement: PadArrangement, numpadModifier: Modifier ->
            currentNumpad(arrangement, numpadModifier)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val arrangement: PadArrangement = if (maxWidth > maxHeight) {
            PadArrangement.SideBySide
        } else {
            PadArrangement.Stacked
        }

        Column(modifier = Modifier.fillMaxSize()) {
            when (arrangement) {
                PadArrangement.Stacked -> {
                    Row(
                        modifier = Modifier.padding(start = spacing.s4),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        movableMenu()
                        movableMonthLine(Modifier.weight(1f))
                    }
                    movableHero(Modifier.weight(1f).fillMaxWidth())
                    movableForm(arrangement)
                    movableNumpad(arrangement, Modifier.fillMaxWidth())
                }

                PadArrangement.SideBySide -> {
                    Row(modifier = Modifier.weight(1f).padding(start = spacing.s4)) {
                        movableMenu()
                        movableHero(Modifier.weight(1f).fillMaxHeight())
                    }
                    Row {
                        Column(modifier = Modifier.weight(1f)) {
                            movableMonthLine(Modifier.fillMaxWidth())
                            movableForm(arrangement)
                        }
                        movableNumpad(arrangement, Modifier.weight(1f))
                    }
                }
            }
            cta()
        }
    }
}
