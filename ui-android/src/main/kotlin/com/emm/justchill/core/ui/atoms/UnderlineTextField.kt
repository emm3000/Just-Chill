package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors

private val UnderlineFieldTextStyle = TextStyle(
    fontSize = 18.sp,
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
) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = UnderlineFieldTextStyle.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.accent),
        singleLine = true,
        interactionSource = interactionSource,
        keyboardOptions = keyboardOptions,
        modifier = modifier.underline(if (isFocused) colors.accentFocus else colors.border),
        decorationBox = { inner -> UnderlineDecoration(placeholder, value.isEmpty(), inner) },
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
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = UnderlineFieldTextStyle.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.accent),
        singleLine = true,
        interactionSource = interactionSource,
        keyboardOptions = keyboardOptions,
        modifier = modifier.underline(if (isFocused) colors.accentFocus else colors.border),
        decorationBox = { inner -> UnderlineDecoration(placeholder, value.text.isEmpty(), inner) },
    )
}

private fun Modifier.underline(color: Color): Modifier = this
    .fillMaxWidth()
    .drawBehind {
        drawLine(
            color = color,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1f,
        )
    }
    .padding(vertical = 8.dp)

@Composable
private fun UnderlineDecoration(placeholder: String, isEmpty: Boolean, innerTextField: @Composable () -> Unit) {
    Box {
        if (isEmpty) {
            Text(
                text = placeholder,
                fontSize = 18.sp,
                fontWeight = FontWeight.W400,
                fontFamily = InterFontFamily,
                color = LocalEmmColors.current.textTertiary,
            )
        }
        innerTextField()
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
                .padding(16.dp),
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
                .padding(16.dp),
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
                .padding(16.dp),
        ) {
            UnderlineTextField(
                value = TextFieldValue("12.50", TextRange(2)),
                onValueChange = {},
                placeholder = "0",
            )
        }
    }
}
