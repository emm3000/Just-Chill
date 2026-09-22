package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
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
        decorationBox = { inner ->
            UnderlineDecoration(
                placeholder = placeholder,
                isEmpty = value.isEmpty(),
                isFocused = isFocused,
                innerTextField = inner,
                trailing = trailing,
            )
        },
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
        decorationBox = { inner ->
            UnderlineDecoration(
                placeholder = placeholder,
                isEmpty = value.text.isEmpty(),
                isFocused = isFocused,
                innerTextField = inner,
                trailing = null,
            )
        },
    )
}

@Composable
private fun UnderlineDecoration(
    placeholder: String,
    isEmpty: Boolean,
    isFocused: Boolean,
    innerTextField: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Layout(
        content = {
            Box(modifier = Modifier.padding(vertical = spacing.s2)) {
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
            Box(
                modifier = Modifier
                    .height(spacing.hairline)
                    .background(if (isFocused) colors.borderFocus else colors.border),
            )
            trailing?.invoke()
        },
    ) { measurables: List<Measurable>, constraints: Constraints ->
        val width: Int = constraints.maxWidth
        val loose: Constraints = constraints.copy(minWidth = 0, minHeight = 0)
        val trailingPlaceables: List<Placeable> = measurables.drop(2).map { it.measure(loose) }
        val trailingWidth: Int = trailingPlaceables.maxOfOrNull { it.width } ?: 0
        val textWidth: Int = (width - trailingWidth).coerceAtLeast(0)
        val text: Placeable = measurables[0].measure(loose.copy(minWidth = textWidth, maxWidth = textWidth))
        val line: Placeable = measurables[1].measure(loose.copy(minWidth = width, maxWidth = width))
        val trailingHeight: Int = trailingPlaceables.maxOfOrNull { it.height } ?: 0
        val height: Int = maxOf(text.height, trailingHeight, constraints.minHeight)
        val textTop: Int = (height - text.height) / 2

        layout(width, height) {
            text.place(0, textTop)
            line.place(0, textTop + text.height - line.height)
            trailingPlaceables.forEach { it.place(width - trailingWidth, (height - it.height) / 2) }
        }
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
