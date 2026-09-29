package com.emm.justchill.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.category.resolvedColor
import com.emm.justchill.core.ui.category.selectableColorIds
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

private val colorLabels: Map<String, String> = mapOf(
    "blue" to "Pizarra",
    "green" to "Salvia",
    "red" to "Terracota",
    "purple" to "Malva",
    "orange" to "Ocre",
    "gray" to "Grafito",
)

@Composable
internal fun ColorRow(selected: String, onSelect: (String) -> Unit) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Row(horizontalArrangement = Arrangement.spacedBy(spacing.s1)) {
        selectableColorIds.forEach { colorId: String ->
            ColorSwatch(
                colorId = colorId,
                selected = colorId == selected,
                onClick = { onSelect(colorId) },
            )
        }
    }
}

@Composable
private fun ColorSwatch(colorId: String, selected: Boolean, onClick: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val ring: Color = if (selected) colors.borderFocus else Color.Transparent
    val label: String = colorLabels[colorId] ?: colorId
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(spacing.s12)
            .border(spacing.hairline, ring, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                this.selected = selected
            },
    ) {
        Box(
            modifier = Modifier
                .size(spacing.s6)
                .clip(CircleShape)
                .background(colors.resolvedColor(colorId))
                .indication(interactionSource, ripple()),
        )
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun ColorRowPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Box(
            modifier = Modifier
                .background(colors.bg)
                .padding(spacing.s4),
        ) {
            ColorRow(selected = "green", onSelect = {})
        }
    }
}
