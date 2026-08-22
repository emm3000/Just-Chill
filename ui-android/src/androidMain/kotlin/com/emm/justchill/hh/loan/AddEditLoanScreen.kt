package com.emm.justchill.hh.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.CtaTone
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.core.ui.atoms.UnderlineTextField
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.shared.AmountInputSheet
import com.emm.justchill.hh.shared.FormSection
import com.emm.justchill.hh.transaction.components.FrequentComboChip
import com.emm.justchill.hh.transaction.sheets.DatePickerSheet
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddEditLoanScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    loanId: String? = null,
    vm: AddEditLoanViewModel = koinViewModel(
        parameters = { parametersOf(loanId) },
    ),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                AddEditLoanEffect.NavigateBack -> currentOnBack()

                is AddEditLoanEffect.ShowError -> snackbarHostState.showEmmSnackbar(
                    message = effect.message,
                    tone = EmmSnackbarTone.Error,
                )
            }
        }
    }

    AddEditLoanContent(
        state = state,
        onIntent = vm::onIntent,
        onBack = currentOnBack,
    )
}

@Composable
private fun AddEditLoanContent(
    state: AddEditLoanUiState,
    onIntent: (AddEditLoanIntent) -> Unit,
    onBack: () -> Unit = {},
) {
    val colors = LocalEmmColors.current

    var showAmountSheet by remember { mutableStateOf(false) }
    var showDateSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        val title = if (state.isEdit) "Editar préstamo" else "Nuevo préstamo"
        JcTopBar(
            title = title,
            left = {
                IconBtn(
                    icon = Icons.Outlined.Close,
                    onClick = onBack,
                )
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            FormSection(eyebrow = "PERSONA") {
                UnderlineTextField(
                    value = state.personName,
                    onValueChange = { onIntent(AddEditLoanIntent.OnPersonNameChange(it)) },
                    placeholder = "Ej. Juan",
                )
                if (state.personSuggestions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.personSuggestions) { name ->
                            FrequentComboChip(
                                label = name,
                                dotColor = null,
                                onClick = { onIntent(AddEditLoanIntent.OnPersonNameChange(name)) },
                            )
                        }
                    }
                }
            }

            FormSection(eyebrow = "MONTO") {
                AmountCard(amountDigits = state.amountDigits, onClick = { showAmountSheet = true })
            }

            FormSection(eyebrow = "INTERÉS %") {
                UnderlineTextField(
                    value = state.interestPercentText,
                    onValueChange = { onIntent(AddEditLoanIntent.OnInterestPercentChange(it)) },
                    placeholder = "0",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            FormSection(eyebrow = "FECHA") {
                DateRow(label = state.dateLabel, onClick = { showDateSheet = true })
            }

            FormSection(eyebrow = "NOTA · OPCIONAL") {
                UnderlineTextField(
                    value = state.note,
                    onValueChange = { onIntent(AddEditLoanIntent.OnNoteChange(it)) },
                    placeholder = "Ej. Prestado en efectivo",
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        val ctaLabel = if (state.isEdit) "Guardar cambios" else "Crear préstamo"
        StickyCTA(
            label = ctaLabel,
            tone = CtaTone.Accent,
            interaction = when {
                state.isSaving -> CtaInteraction.Loading
                state.isSaveEnabled -> CtaInteraction.Enabled
                else -> CtaInteraction.Disabled
            },
            onClick = { onIntent(AddEditLoanIntent.Save) },
        )
    }

    if (showAmountSheet) {
        AmountInputSheet(
            amountDigits = state.amountDigits,
            title = "Monto del préstamo",
            tone = AmountTone.Neutral,
            onAmountConfirm = { onIntent(AddEditLoanIntent.OnAmountChange(it)) },
            onDismiss = { showAmountSheet = false },
        )
    }

    if (showDateSheet) {
        DatePickerSheet(
            currentDate = state.pickerDate,
            onConfirm = { date ->
                onIntent(AddEditLoanIntent.OnDateSelected(date))
                showDateSheet = false
            },
            onDismiss = { showDateSheet = false },
        )
    }
}

@Preview
@Composable
private fun AddEditLoanScreenPreview() {
    EmmTheme {
        AddEditLoanContent(
            state = AddEditLoanUiState(
                today = LocalDate(2026, 8, 21),
                personName = "Juan",
                personSuggestions = listOf("Juan", "María"),
                amountDigits = "20000",
                interestPercentText = "12.5",
                isSaveEnabled = true,
            ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun AddEditLoanScreenEmptyPreview() {
    EmmTheme {
        AddEditLoanContent(
            state = AddEditLoanUiState(today = LocalDate(2026, 8, 21)),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun AddEditLoanScreenEditPreview() {
    EmmTheme {
        AddEditLoanContent(
            state = AddEditLoanUiState(
                today = LocalDate(2026, 8, 21),
                isEdit = true,
                personName = "María",
                amountDigits = "35000",
                interestPercentText = "8",
                note = "Prestado por transferencia",
                isSaveEnabled = true,
            ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun AddEditLoanScreenSavingPreview() {
    EmmTheme {
        AddEditLoanContent(
            state = AddEditLoanUiState(
                today = LocalDate(2026, 8, 21),
                personName = "Juan",
                amountDigits = "20000",
                interestPercentText = "12.5",
                isSaveEnabled = true,
                isSaving = true,
            ),
            onIntent = {},
        )
    }
}
