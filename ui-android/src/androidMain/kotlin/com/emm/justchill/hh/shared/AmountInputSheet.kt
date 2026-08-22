package com.emm.justchill.hh.shared

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.Numpad
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.hh.transaction.MAX_AMOUNT_DIGITS
import com.emm.justchill.hh.transaction.centsToSoles
import com.emm.justchill.hh.transaction.formatCentsForDisplay

/**
 * The numpad edits a draft seeded from [amountDigits]; [onAmountConfirm] fires once, when the
 * confirm CTA commits it. Closing the sheet — affordance, scrim or back gesture — discards the
 * draft, so the owner keeps the amount it had.
 */
// amountDigits/title/tone/onAmountConfirm/onDismiss are the loan and recurring callers' only
// required inputs; modifier is conventional and subtitle the one optional extra the recurring
// sheet adds. Splitting these into a config object would relocate the count, not reduce it.
@Suppress("LongParameterList")
@Composable
fun AmountInputSheet(
    amountDigits: String,
    title: String,
    tone: AmountTone,
    onAmountConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = LocalEmmColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
    ) {
        AmountInputSheetContent(
            amountDigits = amountDigits,
            title = title,
            tone = tone,
            onAmountConfirm = onAmountConfirm,
            onDismiss = onDismiss,
            subtitle = subtitle,
        )
    }
}

// The content mirrors the shell's inputs one for one, plus the conventional modifier.
@Suppress("LongParameterList")
@Composable
private fun AmountInputSheetContent(
    amountDigits: String,
    title: String,
    tone: AmountTone,
    onAmountConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    // Seeded once per opening: every caller renders the sheet inside an `if (show…)`, so leaving
    // the composition drops the draft and the next opening reads the owner's amount again.
    var draftDigits: String by rememberSaveable { mutableStateOf(amountDigits) }
    val draftValue: Double = remember(draftDigits) { centsToSoles(draftDigits) }
    val formattedDraft: String = remember(draftDigits) {
        if (draftDigits.isEmpty()) "0.00" else formatCentsForDisplay(draftDigits)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = title, style = type.titleM, color = colors.textPrimary)
            IconBtn(
                icon = Icons.Outlined.Close,
                onClick = onDismiss,
                contentDescription = "Cerrar",
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AmountHero(
                value = draftValue,
                tone = tone,
                showCaret = true,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = type.caption,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Numpad(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onDigit = { ch -> draftDigits = (draftDigits + ch).take(MAX_AMOUNT_DIGITS) },
            onDoubleZero = { draftDigits = (draftDigits + "00").take(MAX_AMOUNT_DIGITS) },
            onBackspace = { draftDigits = draftDigits.dropLast(1) },
        )

        Spacer(Modifier.height(12.dp))

        val confirmEnabled = draftDigits.isNotEmpty() && draftDigits.toLongOrNull() != 0L
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
                    if (confirmEnabled) {
                        Modifier.clickable {
                            onAmountConfirm(draftDigits)
                            onDismiss()
                        }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            // These three mirror StickyCTA's own inline label — no role holds 15sp Inter W600, and
            // restyling this side alone would split the two CTAs apart.
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
                    text = "S/ $formattedDraft",
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

@Preview
@Composable
private fun AmountInputSheetContentPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg),
        ) {
            AmountInputSheetContent(
                amountDigits = "5000",
                title = "Monto del abono",
                tone = AmountTone.Neutral,
                onAmountConfirm = {},
                onDismiss = {},
                subtitle = "Máximo S/ 160.00",
            )
        }
    }
}
