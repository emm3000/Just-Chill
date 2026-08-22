package com.emm.justchill.hh.loan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.loan.PaymentMethod
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.SegmentOption
import com.emm.justchill.core.ui.atoms.Segmented
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.core.ui.atoms.UnderlineTextField
import com.emm.justchill.hh.shared.AmountInputSheet
import com.emm.justchill.hh.shared.FormSection
import com.emm.justchill.hh.transaction.sheets.DatePickerSheet

@Composable
fun LoanPaymentSheet(form: LoanPaymentFormUi, loanRemaining: String?, onIntent: (LoanDetailIntent) -> Unit) {
    val colors = LocalEmmColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAmountSheet by remember { mutableStateOf(false) }
    var showDateSheet by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentDismiss) },
        sheetState = sheetState,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = "Registrar abono",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W600,
                    fontFamily = InterFontFamily,
                    color = colors.textPrimary,
                    letterSpacing = (-0.15).sp,
                )

                FormSection(eyebrow = "MONTO") {
                    AmountCard(amountDigits = form.amountDigits, onClick = { showAmountSheet = true })
                }

                FormSection(eyebrow = "MÉTODO") {
                    Segmented(
                        options = listOf(
                            SegmentOption(PaymentMethod.Cash, PaymentMethod.Cash.label),
                            SegmentOption(PaymentMethod.Transfer, PaymentMethod.Transfer.label),
                        ),
                        selected = form.method,
                        onSelect = { onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentMethodChange(it)) },
                    )
                }

                FormSection(eyebrow = "FECHA") {
                    DateRow(label = form.dateLabel, onClick = { showDateSheet = true })
                }

                FormSection(eyebrow = "NOTA · OPCIONAL") {
                    UnderlineTextField(
                        value = form.note,
                        onValueChange = { onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentNoteChange(it)) },
                        placeholder = "Ej. Pago en efectivo",
                    )
                }
            }

            StickyCTA(
                label = "Registrar abono",
                interaction = when {
                    form.isSaving -> CtaInteraction.Loading
                    form.isSaveEnabled -> CtaInteraction.Enabled
                    else -> CtaInteraction.Disabled
                },
                onClick = { onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentConfirm) },
            )
        }
    }

    if (showAmountSheet) {
        AmountInputSheet(
            amountDigits = form.amountDigits,
            title = "Monto del abono",
            tone = AmountTone.Neutral,
            subtitle = loanRemaining?.let { "Máximo $it" },
            onAmountChange = { onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentAmountChange(it)) },
            onDismiss = { showAmountSheet = false },
        )
    }

    if (showDateSheet) {
        DatePickerSheet(
            currentDate = form.date ?: form.today,
            onConfirm = { date ->
                onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentDateSelected(date))
                showDateSheet = false
            },
            onDismiss = { showDateSheet = false },
        )
    }
}
