package com.emm.justchill.feature.transaction.capture.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.atoms.ChevronTrailing
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
internal fun MonthSpendLine(label: String, amount: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current

    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val ground: Color = if (isPressed) colors.surface1 else colors.bg
    val description: String = "$label: $amount. Toca para ver tus movimientos."

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ground)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .heightIn(min = spacing.s12)
            .padding(horizontal = spacing.s4)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            Eyebrow(text = label, modifier = Modifier.weight(1f, fill = false))
            Text(text = amount, style = type.amountM, color = colors.textSecondary, maxLines = 1)
        }
        ChevronTrailing()
    }
}

@Preview
@Composable
private fun MonthSpendLinePreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Box(modifier = Modifier.background(colors.bg).padding(spacing.s2)) {
            MonthSpendLine(label = "Gastado en agosto", amount = "S/ 1,240.50", onClick = {})
        }
    }
}
