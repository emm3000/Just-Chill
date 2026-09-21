package com.emm.justchill.core.ui.sheets

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.Numpad
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.CtaHeight
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.core.ui.format.MAX_AMOUNT_DIGITS
import com.emm.justchill.core.ui.format.centsToSoles
import com.emm.justchill.core.ui.format.formatCentsForDisplay
import com.emm.justchill.core.ui.format.sanitizeCentsInput
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

/**
 * The numpad edits a draft seeded from [amountDigits]; [onAmountConfirm] fires once, when the
 * confirm CTA commits it. Closing the sheet — affordance, scrim or back gesture — discards the
 * draft, so the owner keeps the amount it had.
 */
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
    val colors: EmmColors = LocalEmmColors.current
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
    val colors: EmmColors = LocalEmmColors.current
    val radii: EmmRadii = LocalEmmRadii.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    // Seeded once per opening: every caller renders the sheet inside an `if (show…)`, so leaving
    // the composition drops the draft and the next opening reads the owner's amount again.
    var draftDigits: String by rememberSaveable { mutableStateOf(sanitizeCentsInput(amountDigits)) }
    val draftValue: Double = remember(draftDigits) { centsToSoles(draftDigits) }
    val formattedDraft: String = remember(draftDigits) {
        if (draftDigits.isEmpty()) "0.00" else formatCentsForDisplay(draftDigits)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.s6, end = spacing.s4, bottom = spacing.s4),
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
                .padding(bottom = spacing.s1),
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
                    modifier = Modifier.padding(top = spacing.s1),
                )
            }
        }

        Spacer(Modifier.height(spacing.s4))

        Numpad(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4),
            onDigit = { ch -> draftDigits = (draftDigits + ch).take(MAX_AMOUNT_DIGITS) },
            onDoubleZero = { draftDigits = (draftDigits + "00").take(MAX_AMOUNT_DIGITS) },
            onBackspace = { draftDigits = draftDigits.dropLast(1) },
        )

        Spacer(Modifier.height(spacing.s3))

        val confirmEnabled: Boolean = draftDigits.isNotEmpty() && draftDigits.toLongOrNull() != 0L
        val ctaBg: Color = if (confirmEnabled) colors.textPrimary else colors.surface1
        val ctaFg: Color = if (confirmEnabled) colors.bg else colors.textTertiary

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4)
                .padding(bottom = spacing.s4)
                .height(CtaHeight)
                .clip(radii.rL)
                .background(ctaBg)
                .clickable(enabled = confirmEnabled, role = Role.Button) {
                    onAmountConfirm(draftDigits)
                    onDismiss()
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Text(text = "Listo", style = type.titleM, color = ctaFg)
                Text(text = "·", style = type.titleM, color = ctaFg.copy(alpha = 0.6f))
                Text(
                    text = "S/ $formattedDraft",
                    style = type.amountM.copy(fontWeight = FontWeight.W600),
                    color = ctaFg.copy(alpha = 0.9f),
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
