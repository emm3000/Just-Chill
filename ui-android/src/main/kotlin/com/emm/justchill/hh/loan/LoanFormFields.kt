package com.emm.justchill.hh.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.hh.transaction.centsToSoles

private val MIN_TOUCH_TARGET = 48.dp

@Composable
fun AmountCard(amountDigits: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(radii.rM)
            .background(colors.surface1)
            .border(1.dp, colors.border, radii.rM)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Cambiar monto",
                onClick = onClick,
            )
            .padding(vertical = spacing.s5),
        contentAlignment = Alignment.Center,
    ) {
        // One hero for both branches: an empty accumulator reads as 0.00 and the mute tone is what
        // says "nothing typed yet", so the first digit changes neither the typeface nor the height
        // and the card stops shoving the form down.
        AmountHero(
            value = centsToSoles(amountDigits),
            tone = if (amountDigits.isEmpty()) AmountTone.Mute else AmountTone.Neutral,
            showCaret = false,
        )
    }
}

@Composable
fun DateRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            // The padded label alone measures under the 48dp touch floor at a small font scale.
            .heightIn(min = MIN_TOUCH_TARGET)
            .clip(radii.rM)
            .background(colors.surface1)
            .border(1.dp, colors.border, radii.rM)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Cambiar fecha",
                onClick = onClick,
            )
            .padding(horizontal = spacing.s4, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
        Text(text = label, style = type.labelL, color = colors.textPrimary)
    }
}

@Preview
@Composable
private fun AmountCardEmptyPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
        ) {
            AmountCard(amountDigits = "", onClick = {})
        }
    }
}

@Preview
@Composable
private fun AmountCardFilledPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
        ) {
            AmountCard(amountDigits = "120000", onClick = {})
        }
    }
}

@Preview
@Composable
private fun DateRowPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
        ) {
            DateRow(label = "22 de agosto de 2026", onClick = {})
        }
    }
}
