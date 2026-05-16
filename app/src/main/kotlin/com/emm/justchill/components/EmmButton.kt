package com.emm.justchill.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

/**
 * Design system button. Mirrors `docs/DESIGN_SYSTEM.md §7.1`.
 */
enum class EmmButtonVariant { Primary, Secondary, Ghost, Destructive }

@Composable
fun EmmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: EmmButtonVariant = EmmButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bg: Color = when {
        !enabled -> colors.surface1
        variant == EmmButtonVariant.Primary -> colors.accent
        variant == EmmButtonVariant.Secondary -> colors.surface1
        else -> Color.Transparent
    }

    val textColor: Color = when {
        !enabled -> colors.textDisabled
        variant == EmmButtonVariant.Primary -> colors.textOnAccent
        variant == EmmButtonVariant.Destructive -> colors.danger
        else -> colors.textPrimary
    }

    val border: BorderStroke? = when {
        !enabled -> null
        variant == EmmButtonVariant.Secondary -> BorderStroke(1.dp, colors.border)
        variant == EmmButtonVariant.Destructive -> BorderStroke(1.dp, colors.danger)
        else -> null
    }

    val pressOverlay: Color = if (isPressed && enabled) Color.Black.copy(alpha = 0.08f) else Color.Transparent

    Row(
        modifier = modifier
            .clip(radii.rS)
            .background(bg)
            .then(border?.let { Modifier.border(it, radii.rS) } ?: Modifier)
            .background(pressOverlay)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = spacing.s5),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = type.labelL,
                color = textColor,
            )
        }
    }
}

