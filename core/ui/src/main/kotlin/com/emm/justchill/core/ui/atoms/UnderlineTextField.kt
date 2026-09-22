package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.InterFontFamily
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

// No EmmType role is Inter 18sp; the input and its placeholder share this size until the field takes a role.
private val UnderlineFieldFontSize: TextUnit = 18.sp

private val UnderlineFieldTextStyle = TextStyle(
    fontSize = UnderlineFieldFontSize,
    fontWeight = FontWeight.W500,
    fontFamily = InterFontFamily,
    letterSpacing = (-0.18).sp,
)

@Composable
fun UnderlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors: EmmColors = LocalEmmColors.current
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isFocused: Boolean by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = UnderlineFieldTextStyle.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.borderFocus),
        singleLine = true,
        interactionSource = interactionSource,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner -> UnderlineDecoration(placeholder, value.isEmpty(), isFocused, inner, trailing) },
    )
}

/**
 * The [TextFieldValue] form, for a field whose owner rewrites what was typed: only a caller holding
 * the caret can say where it lands once the rewrite arrives. Anything the owner passes through
 * untouched takes the [String] overload, where the framework keeps the caret itself.
 */
@Composable
fun UnderlineTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors: EmmColors = LocalEmmColors.current
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isFocused: Boolean by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = UnderlineFieldTextStyle.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.borderFocus),
        singleLine = true,
        interactionSource = interactionSource,
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner -> UnderlineDecoration(placeholder, value.text.isEmpty(), isFocused, inner, trailing = null) },
    )
}

private fun Modifier.underline(color: Color, spacing: EmmSpacing): Modifier = this
    .drawBehind {
        val strokeWidth: Float = spacing.hairline.toPx()
        val centerY: Float = size.height - strokeWidth / 2
        drawLine(
            color = color,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = strokeWidth,
        )
    }
    .padding(vertical = spacing.s2)

@Composable
private fun UnderlineDecoration(
    placeholder: String,
    isEmpty: Boolean,
    isFocused: Boolean,
    innerTextField: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    val colors: EmmColors = LocalEmmColors.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .underline(if (isFocused) colors.borderFocus else colors.border, LocalEmmSpacing.current),
        ) {
            if (isEmpty) {
                Text(
                    text = placeholder,
                    fontSize = UnderlineFieldFontSize,
                    fontWeight = FontWeight.W400,
                    fontFamily = InterFontFamily,
                    color = colors.textTertiary,
                )
            }
            innerTextField()
        }
        trailing?.invoke()
    }
}

@Preview
@Composable
private fun UnderlineTextFieldEmptyPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(LocalEmmSpacing.current.s4),
        ) {
            UnderlineTextField(value = "", onValueChange = {}, placeholder = "Ej. Juan")
        }
    }
}

@Preview
@Composable
private fun UnderlineTextFieldFilledPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(LocalEmmSpacing.current.s4),
        ) {
            UnderlineTextField(value = "Juan", onValueChange = {}, placeholder = "Ej. Juan")
        }
    }
}

@Preview
@Composable
private fun UnderlineTextFieldCaretPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(LocalEmmSpacing.current.s4),
        ) {
            UnderlineTextField(
                value = TextFieldValue("12.50", TextRange(2)),
                onValueChange = {},
                placeholder = "0",
            )
        }
    }
}

@Preview
@Composable
private fun UnderlineTextFieldMaskedPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(LocalEmmSpacing.current.s4),
        ) {
            UnderlineTextField(
                value = "password123",
                onValueChange = {},
                placeholder = "Mínimo 8 caracteres",
                visualTransformation = PasswordVisualTransformation(),
                trailing = {
                    Text(
                        text = "Ver",
                        style = LocalEmmType.current.labelM,
                        color = LocalEmmColors.current.textTertiary,
                    )
                },
            )
        }
    }
}

@Preview
@Composable
private fun UnderlineTextFieldTouchTargetTrailingPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(LocalEmmSpacing.current.s4),
        ) {
            UnderlineTextField(
                value = "password123",
                onValueChange = {},
                placeholder = "Mínimo 8 caracteres",
                visualTransformation = PasswordVisualTransformation(),
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(LocalEmmSpacing.current.s12)
                            .background(LocalEmmColors.current.surface1),
                    )
                },
            )
        }
    }
}
