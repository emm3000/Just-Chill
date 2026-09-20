package com.emm.justchill.feature.transaction.capture.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

// Both segments hold the same width so the pill keeps its size and its centre when the selection
// moves; sizing each to its own label would shift "Ingreso"/"Gasto" sideways on every tap.
private val SignSegmentWidth: Dp = 96.dp

/**
 * Selection reads through the text ladder and one surface step — never a tint. An expense is not
 * red and an income is not green here; the amount above carries that.
 */
@Composable
internal fun SignToggle(
    isSpend: Boolean,
    onIncomeClick: () -> Unit,
    onSpendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = modifier
            .height(spacing.s12)
            .clip(radii.rFull)
            .background(colors.bg)
            .border(1.dp, colors.border, radii.rFull),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SignSegment(label = "Ingreso", selected = !isSpend, onClick = onIncomeClick)
        SignSegment(label = "Gasto", selected = isSpend, onClick = onSpendClick)
    }
}

@Composable
private fun SignSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    // The target is the whole segment; the fill sits inset inside it, so a tap on the pill's edge
    // still selects. Selection is a surface step and a weight — neither reaches TalkBack, so it
    // is stated.
    Box(
        modifier = Modifier
            .width(SignSegmentWidth)
            .fillMaxHeight()
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.s1)
                .clip(radii.rFull)
                .background(if (selected) colors.surface2 else Color.Transparent)
                .indication(interactionSource, ripple()),
        ) {
            Text(
                text = label,
                style = type.labelL,
                fontWeight = if (selected) FontWeight.W600 else FontWeight.W500,
                color = if (selected) colors.textPrimary else colors.textTertiary,
            )
        }
    }
}

// The two chips share a row and not its width: a category name ("Supermercado") is the longest
// label the form carries, an account is a bank's four letters.
internal const val ACCOUNT_CHIP_WEIGHT: Float = 1f
internal const val CATEGORY_CHIP_WEIGHT: Float = 1.6f
