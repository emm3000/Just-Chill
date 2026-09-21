package com.emm.justchill.core.ui.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.Numpad
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

private const val MAX_CENTS_DIGITS = 9
private const val DOUBLE_ZERO_MULTIPLIER = 100L
private const val DIGIT_SHIFT = 10L

/**
 * Confirming or skipping never dismisses: the caller closes the sheet once the operation succeeds.
 */
@Composable
fun ConfirmRecurringSheet(
    item: PendingRecurringUi,
    onConfirm: (callerAmount: Money?) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var amountCents: Long by rememberSaveable(item.id) { mutableLongStateOf(item.fixedAmountCents ?: 0L) }

    val amountDouble: Double = amountCents.toDouble() / 100.0
    val tone: AmountTone = if (item.type == TransactionType.Income) AmountTone.Pos else AmountTone.Neutral

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s6, vertical = spacing.s1),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.name,
                    style = type.titleM,
                    color = colors.textPrimary,
                )
                TypeBadge(type = item.type)
            }

            Spacer(Modifier.height(spacing.s2))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s6),
                horizontalArrangement = Arrangement.spacedBy(spacing.s4),
            ) {
                Column {
                    Eyebrow(text = "Día")
                    Spacer(Modifier.height(spacing.s1))
                    Text(
                        text = item.dayOfMonth.toString(),
                        style = type.labelL,
                        color = colors.textPrimary,
                    )
                }

                Spacer(Modifier.width(spacing.s2))
                Column {
                    Eyebrow(text = if (item.isCatchUp) "Mes atrasado" else "Mes")
                    Spacer(Modifier.height(spacing.s1))
                    Text(
                        text = item.periodLabel,
                        style = type.labelL,
                        color = if (item.isCatchUp) colors.danger else colors.textPrimary,
                    )
                }

                if (item.description.isNotBlank()) {
                    Spacer(Modifier.width(spacing.s2))
                    Column {
                        Eyebrow(text = "Nota")
                        Spacer(Modifier.height(spacing.s1))
                        Text(
                            text = item.description,
                            style = type.bodyM,
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(spacing.s5))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s6),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AmountHero(
                    value = amountDouble,
                    tone = tone,
                    showCaret = item.isVariableAmount,
                )
            }

            Spacer(Modifier.height(spacing.s3))

            if (item.isVariableAmount) {
                Numpad(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.s3),
                    onDigit = { ch ->
                        val digit: Long = ch.digitToInt().toLong()
                        val shifted: Long = amountCents * DIGIT_SHIFT + digit
                        if (shifted.toString().length <= MAX_CENTS_DIGITS) amountCents = shifted
                    },
                    onDoubleZero = {
                        val shifted: Long = amountCents * DOUBLE_ZERO_MULTIPLIER
                        if (shifted.toString().length <= MAX_CENTS_DIGITS) amountCents = shifted
                    },
                    onBackspace = { amountCents = amountCents / DIGIT_SHIFT },
                )
                Spacer(Modifier.height(spacing.s1))
            } else {
                Spacer(Modifier.height(spacing.s3))
            }

            val confirmEnabled: Boolean = amountCents > 0L
            StickyCTA(
                label = "Confirmar",
                sublabel = if (!confirmEnabled && item.isVariableAmount) "Ingresa el monto" else null,
                interaction = if (confirmEnabled) CtaInteraction.Enabled else CtaInteraction.Disabled,
                onClick = { onConfirm(if (item.isVariableAmount) Money(amountCents) else null) },
            )

            SkipPeriodAction(periodLabel = item.periodLabel, onSkip = onSkip)
        }
    }
}

/**
 * Plain text under the CTA, never a second button: skipping discards a month with nothing
 * booked, and equal visual weight would invite tapping past one that should have been recorded.
 */
@Composable
private fun SkipPeriodAction(periodLabel: String, onSkip: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val emmType: EmmType = LocalEmmType.current
    Text(
        text = "No lo pagué en $periodLabel",
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSkip)
            .padding(vertical = spacing.s4),
        textAlign = TextAlign.Center,
        style = emmType.labelL,
        color = colors.textSecondary,
    )
}

@Composable
private fun TypeBadge(type: TransactionType) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val emmType: EmmType = LocalEmmType.current
    val badgeShape: RoundedCornerShape = radii.rXS
    val label: String = type.label
    val bgColor: Color = when (type) {
        TransactionType.Income -> colors.posMuted
        TransactionType.Spend -> colors.negMuted
    }
    val textColor: Color = when (type) {
        TransactionType.Income -> colors.success
        TransactionType.Spend -> colors.danger
    }
    Text(
        text = label,
        modifier = Modifier
            .background(bgColor, badgeShape)
            .padding(horizontal = spacing.s2, vertical = spacing.s1),
        style = emmType.caption.copy(fontWeight = FontWeight.W600),
        color = textColor,
    )
}
