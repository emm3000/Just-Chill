package com.emm.justchill.hh.recurring

import androidx.compose.foundation.background
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.shared.Money
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.Numpad
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.core.ui.atoms.StickyCTA

private const val MAX_CENTS_DIGITS = 9
private const val DOUBLE_ZERO_MULTIPLIER = 100L
private const val DIGIT_SHIFT = 10L

/**
 * Bottom sheet to confirm a pending recurring movement.
 *
 * - Fixed amount: shows the amount read-only; "Confirmar" is always enabled.
 * - Variable amount: shows an editable Numpad-driven amount field;
 *   "Confirmar" is disabled while the entered amount is 0 (Scenario 5.1).
 *
 * Sheet dismissal on success is driven by the caller (via [HomeEffect.CloseConfirmSheet]).
 * [onDismiss] is only invoked when the user swipes down or taps outside (user-initiated).
 */
@Composable
fun ConfirmRecurringSheet(
    item: PendingRecurringUi,
    onConfirm: (templateId: String, callerAmount: Money?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Cents accumulator — starts at the fixed amount (if any) or 0 for variable.
    // Keyed by templateId so the amount resets when a different item is shown.
    var amountCents by rememberSaveable(item.templateId) { mutableLongStateOf(item.fixedAmountCents ?: 0L) }

    val amountDouble = amountCents.toDouble() / 100.0
    val tone = when (item.type) {
        TransactionType.Income -> AmountTone.Pos
        TransactionType.Spend -> AmountTone.Neg
    }

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
            // ---- Header ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.name,
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W600,
                        letterSpacing = (-0.2).sp,
                    ),
                    color = colors.textPrimary,
                )
                TypeBadge(type = item.type)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column {
                    Eyebrow(text = "Día")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.dayOfMonth.toString(),
                        style = TextStyle(
                            fontFamily = InterFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W500,
                        ),
                        color = colors.textPrimary,
                    )
                }

                if (item.description.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Eyebrow(text = "Nota")
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.description,
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W400,
                            ),
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- Amount display ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AmountHero(
                    value = amountDouble,
                    tone = tone,
                    showCaret = item.isVariableAmount,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ---- Numpad (only for variable amounts) ----
            if (item.isVariableAmount) {
                Numpad(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    onDigit = { ch ->
                        val digit = ch.digitToInt().toLong()
                        val shifted = amountCents * DIGIT_SHIFT + digit
                        if (shifted.toString().length <= MAX_CENTS_DIGITS) amountCents = shifted
                    },
                    onDoubleZero = {
                        val shifted = amountCents * DOUBLE_ZERO_MULTIPLIER
                        if (shifted.toString().length <= MAX_CENTS_DIGITS) amountCents = shifted
                    },
                    onBackspace = { amountCents = amountCents / DIGIT_SHIFT },
                )
                Spacer(Modifier.height(4.dp))
            } else {
                Spacer(Modifier.height(12.dp))
            }

            // ---- CTA ----
            // onDismiss() is NOT called here — the sheet stays open until the VM emits
            // CloseConfirmSheet (success) so errors keep the sheet open with snackbar feedback.
            val confirmEnabled = amountCents > 0L
            StickyCTA(
                label = "Confirmar",
                sublabel = if (!confirmEnabled && item.isVariableAmount) "Ingresá el monto" else null,
                enabled = confirmEnabled,
                onClick = {
                    onConfirm(
                        item.templateId,
                        if (item.isVariableAmount) Money(amountCents) else null,
                    )
                },
            )
        }
    }
}

@Composable
private fun TypeBadge(type: TransactionType) {
    val colors = LocalEmmColors.current
    val label = when (type) {
        TransactionType.Income -> "Ingreso"
        TransactionType.Spend -> "Gasto"
    }
    val bgColor = when (type) {
        TransactionType.Income -> colors.posMuted
        TransactionType.Spend -> colors.negMuted
    }
    val textColor = when (type) {
        TransactionType.Income -> colors.success
        TransactionType.Spend -> colors.danger
    }
    Text(
        text = label,
        modifier = Modifier
            .background(bgColor, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = TextStyle(
            fontFamily = InterFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.W600,
            letterSpacing = 0.2.sp,
        ),
        color = textColor,
    )
}
