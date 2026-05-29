package com.emm.justchill.hh.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.Numpad
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.hh.transaction.MAX_AMOUNT_DIGITS
import com.emm.justchill.hh.transaction.centsToSoles
import com.emm.justchill.hh.transaction.formatCentsForDisplay

/**
 * Bottom sheet for entering a fixed amount via the cents-accumulation numpad.
 *
 * The keypad drives [onAmountChange] with the raw digits string; the caller owns all state.
 * No decimal key — intentional: the proven cents model is preserved.
 * Dismisses when the user taps "Listo" (only enabled when a non-zero amount is set).
 */
@Composable
fun AmountInputSheet(
    amountDigits: String,
    type: TransactionType,
    onAmountChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tone = if (type == TransactionType.Income) AmountTone.Pos else AmountTone.Neg
    val typeLabel = if (type == TransactionType.Income) "Ingreso" else "Gasto"
    val formattedAmount = if (amountDigits.isEmpty()) "0.00" else formatCentsForDisplay(amountDigits)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Monto del recurrente",
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = colors.textPrimary,
                letterSpacing = (-0.15).sp,
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.surface1)
                    .border(1.dp, colors.border, CircleShape)
                    .clickable(onClick = onDismiss),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cerrar",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        // Big centered amount
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AmountHero(
                value = centsToSoles(amountDigits),
                tone = tone,
                showCaret = true,
            )
            Text(
                text = "$typeLabel · se paga cada mes",
                fontSize = 12.sp,
                fontWeight = FontWeight.W400,
                fontFamily = InterFontFamily,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Numpad
        Numpad(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onDigit = { ch ->
                val newDigits = (amountDigits + ch).take(MAX_AMOUNT_DIGITS)
                onAmountChange(newDigits)
            },
            onDoubleZero = {
                val newDigits = (amountDigits + "00").take(MAX_AMOUNT_DIGITS)
                onAmountChange(newDigits)
            },
            onBackspace = {
                onAmountChange(amountDigits.dropLast(1))
            },
        )

        Spacer(Modifier.height(12.dp))

        // Confirm button — enabled only when amount is non-zero
        val confirmEnabled = amountDigits.isNotEmpty() && amountDigits.toLongOrNull() != 0L
        val ctaBg = if (confirmEnabled) colors.accent else colors.surface1
        val ctaFg = if (confirmEnabled) colors.textOnAccent else colors.textTertiary

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ctaBg)
                .then(
                    if (confirmEnabled) Modifier.clickable(onClick = onDismiss) else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Listo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    fontFamily = InterFontFamily,
                    color = ctaFg,
                    letterSpacing = (-0.15).sp,
                )
                Text(
                    text = "·",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    fontFamily = InterFontFamily,
                    color = ctaFg.copy(alpha = 0.6f),
                )
                Text(
                    text = "S/ $formattedAmount",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    fontFamily = InterFontFamily,
                    color = ctaFg.copy(alpha = 0.9f),
                    letterSpacing = (-0.15).sp,
                )
            }
        }
    }
}
