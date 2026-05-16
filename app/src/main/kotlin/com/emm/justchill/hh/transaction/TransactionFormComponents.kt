package com.emm.justchill.hh.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import java.math.BigDecimal
import java.text.DecimalFormat

@Composable
internal fun TypeToggle(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit,
) {
    val spacing = LocalEmmSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.s6),
    ) {
        TypeOption("INGRESO", isSelected = selected == TransactionType.Income) { onSelect(TransactionType.Income) }
        TypeOption("GASTO", isSelected = selected == TransactionType.Spend) { onSelect(TransactionType.Spend) }
    }
}

@Composable
private fun TypeOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val interactionSource = remember { MutableInteractionSource() }

    val underlineColor = if (isSelected) colors.accentFocus else colors.border
    val labelColor = if (isSelected) colors.textPrimary else colors.textTertiary

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = spacing.s2)
            .drawBehind {
                val stroke = if (isSelected) 2f else 1f
                drawLine(
                    color = underlineColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = stroke,
                )
            }
            .padding(bottom = spacing.s2),
    ) {
        Text(text = label, style = type.labelL, color = labelColor)
    }
}

@Composable
internal fun AmountHeroInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    type: TransactionType,
    focusRequester: FocusRequester,
    onNext: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val emmType = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val amount = value.text.replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO
    val isZero = amount == BigDecimal("0.00")
    val sign = if (type == TransactionType.Income) "+" else "−"
    val numberColor = if (isZero) colors.textTertiary else colors.textPrimary
    val prefixColor = if (isZero) colors.textTertiary else colors.textSecondary

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Text(text = "MONTO", style = emmType.labelM, color = colors.textTertiary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${sign}S/",
                style = emmType.amountL,
                color = prefixColor,
                modifier = Modifier.padding(end = spacing.s2, bottom = 4.dp),
            )
            BasicTextField(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                value = value,
                onValueChange = { onValueChange(formatInputToAmount(it)) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { onNext() }),
                textStyle = emmType.amountHero.copy(color = numberColor),
                cursorBrush = SolidColor(colors.accentFocus),
                singleLine = true,
            )
        }
    }
}

@Composable
internal fun ClickableRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    emphasized: Boolean = true,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = spacing.s3)
            .drawBehind {
                drawLine(
                    color = colors.border,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f,
                )
            },
    ) {
        Text(text = label, style = type.labelM, color = colors.textTertiary)
        Spacer(Modifier.height(spacing.s1))
        Text(
            text = value,
            style = type.bodyL,
            color = if (emphasized) colors.textPrimary else colors.textTertiary,
        )
    }
}

internal fun formatInputToAmount(input: TextFieldValue): TextFieldValue {
    val filtered = input.text.filter { it.isDigit() }
    val amount: Long = if (filtered.isEmpty()) 0 else filtered.toLong()
    val formatted = DecimalFormat("#,##0.00").format(amount / 100.0)
    return input.copy(text = formatted, selection = TextRange(formatted.length))
}
