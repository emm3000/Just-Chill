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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

private data class EmmButtonStyle(val background: Color, val textColor: Color, val border: BorderStroke?)

@Composable
fun EmmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: EmmButtonVariant = EmmButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val style = when {
        !enabled -> EmmButtonStyle(colors.surface1, colors.textDisabled, border = null)

        variant == EmmButtonVariant.Primary -> EmmButtonStyle(colors.accent, colors.textOnAccent, border = null)

        variant == EmmButtonVariant.Secondary ->
            EmmButtonStyle(colors.surface1, colors.textPrimary, BorderStroke(1.dp, colors.border))

        variant == EmmButtonVariant.Destructive ->
            EmmButtonStyle(Color.Transparent, colors.danger, BorderStroke(1.dp, colors.danger))

        else -> EmmButtonStyle(Color.Transparent, colors.textPrimary, border = null)
    }

    val pressOverlay: Color = if (isPressed && enabled) Color.Black.copy(alpha = 0.08f) else Color.Transparent

    Row(
        modifier = modifier
            .clip(radii.rS)
            .background(style.background)
            .then(style.border?.let { Modifier.border(it, radii.rS) } ?: Modifier)
            .background(pressOverlay)
            .clickable(
                enabled = enabled && !isLoading,
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
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = style.textColor,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = text,
                    style = type.labelL,
                    color = style.textColor,
                )
            }
        }
    }
}
