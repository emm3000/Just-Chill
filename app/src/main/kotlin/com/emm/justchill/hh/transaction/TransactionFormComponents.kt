package com.emm.justchill.hh.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

@Composable
internal fun AmountHeroInput(
    rawCents: String,
    onRawCentsChange: (String) -> Unit,
    type: TransactionType,
    focusRequester: FocusRequester,
    onNext: () -> Unit,
    onTypeFlip: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val emmType = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val isZero = rawCents.isEmpty() || rawCents.all { it == '0' }
    val sign = if (type == TransactionType.Income) "+" else "−"
    val numberColor = if (isZero) colors.textTertiary else colors.textPrimary
    val currencyColor = if (isZero) colors.textTertiary else colors.textSecondary

    val cursorBrush = remember(colors.accentFocus) { SolidColor(colors.accentFocus) }
    val visualTransformation = remember { CentsVisualTransformation() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Text(text = "MONTO", style = emmType.labelM, color = colors.textTertiary)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            SignChip(sign = sign, onClick = onTypeFlip)
            Row {
                Text(
                    text = "S/",
                    style = emmType.amountL,
                    color = currencyColor,
                    modifier = Modifier
                        .alignByBaseline()
                        .padding(end = spacing.s2),
                )
                BasicTextField(
                    modifier = Modifier
                        .alignByBaseline()
                        .focusRequester(focusRequester),
                    value = rawCents,
                    onValueChange = { newRaw ->
                        val sanitized = sanitizeCentsInput(newRaw)
                        if (sanitized != rawCents) onRawCentsChange(sanitized)
                    },
                    visualTransformation = visualTransformation,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { onNext() }),
                    textStyle = emmType.amountHero.copy(color = numberColor),
                    cursorBrush = cursorBrush,
                    singleLine = true,
                )
            }
        }
    }
}

@Composable
internal fun SignChip(sign: String, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.surface1)
            .border(1.dp, colors.border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = sign,
            style = type.headlineM,
            color = colors.textPrimary,
        )
    }
}

private class CentsVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val display = formatCentsForDisplay(raw)
        return TransformedText(
            text = AnnotatedString(display),
            offsetMapping = EndAnchorOffsetMapping(
                visualLength = display.length,
                rawLength = raw.length,
            ),
        )
    }
}

private class EndAnchorOffsetMapping(
    private val visualLength: Int,
    private val rawLength: Int,
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int = visualLength
    override fun transformedToOriginal(offset: Int): Int = rawLength
}

@Composable
internal fun MetaChip(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = true,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(radii.rS)
            .background(colors.surface1)
            .border(1.dp, colors.border, radii.rS)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = spacing.s3, vertical = spacing.s3),
    ) {
        Text(text = label, style = type.labelM, color = colors.textTertiary)
        Spacer(Modifier.height(spacing.s1))
        Text(
            text = value,
            style = type.bodyL,
            color = if (emphasized) colors.textPrimary else colors.textTertiary,
            maxLines = 1,
        )
    }
}
