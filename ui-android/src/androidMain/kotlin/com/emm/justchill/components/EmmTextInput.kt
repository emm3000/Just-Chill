package com.emm.justchill.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

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
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val stroke = if (isFocused || isError) 2.dp.toPx() else 1.dp.toPx()
                    drawLine(
                        color = underlineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = stroke,
                    )
                }
                .padding(vertical = spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(Modifier.width(spacing.s2))
            }
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = type.bodyL.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accentFocus),
                    interactionSource = interactionSource,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    modifier = if (focusRequester != null) {
                        Modifier.fillMaxWidth().focusRequester(focusRequester)
                    } else {
                        Modifier.fillMaxWidth()
                    },
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
            if (trailingContent != null) {
                Spacer(Modifier.height(0.dp))
                trailingContent()
            }
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
