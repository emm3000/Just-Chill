package com.emm.justchill.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

/**
 * Design system text input. Mirrors `docs/DESIGN_SYSTEM.md §7.2`.
 *
 * Underline-only (no filled background). Label sits above. Focus thickens the underline
 * to `accentFocus`; error swaps to `danger` and shows a helper line.
 */
@Composable
fun EmmTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helper: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val underlineColor by animateColorAsState(
        targetValue = when {
            isError -> colors.danger
            isFocused -> colors.accentFocus
            else -> colors.border
        },
        label = "underline",
    )

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = type.labelM,
                color = if (isError) colors.danger else colors.textSecondary,
            )
            Spacer(Modifier.height(spacing.s2))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val stroke = if (isFocused || isError) 2f else 1f
                    drawLine(
                        color = underlineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = stroke,
                    )
                }
                .padding(vertical = spacing.s3),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                textStyle = type.bodyL.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accentFocus),
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = visualTransformation,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = type.bodyL,
                            color = colors.textTertiary,
                        )
                    }
                    inner()
                },
            )
        }

        if (helper != null) {
            Spacer(Modifier.height(spacing.s1))
            Text(
                text = helper,
                style = type.caption,
                color = if (isError) colors.danger else colors.textTertiary,
            )
        }
    }
}
