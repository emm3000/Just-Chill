package com.emm.justchill.hh.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.loan.PaymentMethod
import com.emm.justchill.core.theme.EmmTheme
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
import kotlinx.datetime.LocalDate

@Composable
fun LoanPaymentSheet(form: LoanPaymentFormUi, onIntent: (LoanDetailIntent) -> Unit) {
    val colors = LocalEmmColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAmountSheet by remember { mutableStateOf(false) }
    var showDateSheet by remember { mutableStateOf(false) }
    // A save already in flight registers the abono whatever happens here, so every gesture the
    // user reads as "abort" — swipe, scrim, back — has to stop until it lands.
    val dismissible = !form.isSaving

    ModalBottomSheet(
        onDismissRequest = { onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentDismiss) },
        sheetState = sheetState,
        sheetGesturesEnabled = dismissible,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = dismissible,
            shouldDismissOnClickOutside = dismissible,
        ),
    ) {
        LoanPaymentSheetContent(
            form = form,
            onAmountClick = { showAmountSheet = true },
            onDateClick = { showDateSheet = true },
            onIntent = onIntent,
        )
    }

    if (showAmountSheet) {
        AmountInputSheet(
            amountDigits = form.amountDigits,
            title = "Monto del abono",
            tone = AmountTone.Neutral,
            onAmountConfirm = { onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentAmountChange(it)) },
            onDismiss = { showAmountSheet = false },
            subtitle = form.maxAmountLabel?.let { "Máximo $it" },
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

@Composable
private fun LoanPaymentSheetContent(
    form: LoanPaymentFormUi,
    onAmountClick: () -> Unit,
    onDateClick: () -> Unit,
    onIntent: (LoanDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val sheetLabel = if (form.editingPaymentId != null) "Editar abono" else "Registrar abono"

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = sheetLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = colors.textPrimary,
                letterSpacing = (-0.15).sp,
            )

            FormSection(eyebrow = "MONTO") {
                AmountCard(amountDigits = form.amountDigits, onClick = onAmountClick)
                form.amountError?.let { message ->
                    Text(
                        text = message,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = InterFontFamily,
                        color = colors.danger,
                    )
                }
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
                DateRow(label = form.dateLabel, onClick = onDateClick)
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
            label = sheetLabel,
            interaction = when {
                form.isSaving -> CtaInteraction.Loading
                form.isSaveEnabled -> CtaInteraction.Enabled
                else -> CtaInteraction.Disabled
            },
            onClick = { onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentConfirm) },
        )
    }
}

@Preview
@Composable
private fun LoanPaymentSheetContentPreview() {
    LoanPaymentSheetContentPreviewFrame(
        form = LoanPaymentFormUi(
            loanId = "loan-1",
            today = LocalDate(2026, 8, 21),
            remainingCents = 16_000L,
            amountDigits = "5000",
            note = "Abono en efectivo",
        ),
    )
}

@Preview
@Composable
private fun LoanPaymentSheetContentOverRemainingPreview() {
    LoanPaymentSheetContentPreviewFrame(
        form = LoanPaymentFormUi(
            loanId = "loan-1",
            today = LocalDate(2026, 8, 21),
            remainingCents = 16_000L,
            amountDigits = "50000",
        ),
    )
}

@Composable
private fun LoanPaymentSheetContentPreviewFrame(form: LoanPaymentFormUi) {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // The body is weighted; an unbounded preview measures it against zero and renders empty.
                .height(560.dp)
                .background(LocalEmmColors.current.bg),
        ) {
            LoanPaymentSheetContent(
                form = form,
                onAmountClick = {},
                onDateClick = {},
                onIntent = {},
            )
        }
    }
}
