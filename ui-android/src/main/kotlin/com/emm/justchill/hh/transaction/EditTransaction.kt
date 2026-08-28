package com.emm.justchill.hh.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.Numpad
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.CtaTone
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.IconBtnTone
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.account.accountDotColor
import com.emm.justchill.hh.transaction.components.ACCOUNT_CHIP_WEIGHT
import com.emm.justchill.hh.transaction.components.CATEGORY_CHIP_WEIGHT
import com.emm.justchill.hh.transaction.components.FormMetaRow
import com.emm.justchill.hh.transaction.components.SelectorChip
import com.emm.justchill.hh.transaction.components.SignToggle
import com.emm.justchill.hh.transaction.sheets.AccountPickerSheet
import com.emm.justchill.hh.transaction.sheets.CategoryPickerSheet
import com.emm.justchill.hh.transaction.sheets.DatePickerSheet
import com.emm.justchill.hh.transaction.sheets.NoteSheet
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditTransaction(
    transactionId: String,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onAddNewAccount: () -> Unit = {},
    vm: EditTransactionViewModel = koinViewModel(parameters = { parametersOf(transactionId) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                EditTransactionEffect.TransactionUpdated -> currentOnBack()

                EditTransactionEffect.TransactionDeleted -> currentOnBack()

                is EditTransactionEffect.ShowError -> snackbarHostState.showEmmSnackbar(
                    message = effect.message,
                    tone = EmmSnackbarTone.Error,
                )
            }
        }
    }

    EditTransactionContent(
        state = state,
        onIntent = vm::onIntent,
        onBack = onBack,
        onAddNewAccount = onAddNewAccount,
    )
}

@Composable
private fun EditTransactionContent(
    state: EditTransactionUiState,
    onIntent: (EditTransactionIntent) -> Unit,
    onBack: () -> Unit,
    onAddNewAccount: () -> Unit = {},
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    var showDeleteDialog by remember { mutableStateOf(false) }

    val isSpend = state.transactionType == TransactionType.Spend
    val title = if (isSpend) "Editar gasto" else "Editar ingreso"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        JcTopBar(
            title = title,
            left = {
                IconBtn(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Volver",
                )
            },
            right = {
                IconBtn(
                    icon = Icons.Outlined.Delete,
                    onClick = { showDeleteDialog = true },
                    contentDescription = if (isSpend) "Eliminar gasto" else "Eliminar ingreso",
                    tone = IconBtnTone.Danger,
                )
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4),
            horizontalArrangement = Arrangement.Center,
        ) {
            SignToggle(
                isSpend = isSpend,
                onIncomeClick = { onIntent(EditTransactionIntent.OnTransactionTypeChange(TransactionType.Income)) },
                onSpendClick = { onIntent(EditTransactionIntent.OnTransactionTypeChange(TransactionType.Spend)) },
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s6)
                .padding(top = spacing.s8, bottom = spacing.s6),
        ) {
            AmountHero(
                value = centsToSoles(state.amount),
                size = type.amountL.fontSize,
                tone = if (isSpend) AmountTone.Neutral else AmountTone.Pos,
                showCaret = true,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            SelectorChip(
                label = state.accountSelected?.name ?: "—",
                dotColor = state.accountSelected?.let { accountDotColor(it.name, colors) },
                onClick = { onIntent(EditTransactionIntent.OnSheetRequested(TransactionSheet.Account)) },
                modifier = Modifier.weight(ACCOUNT_CHIP_WEIGHT),
            )

            SelectorChip(
                label = state.categorySelected?.name ?: "—",
                dotColor = state.categorySelected?.resolvedColor?.primary,
                onClick = { onIntent(EditTransactionIntent.OnSheetRequested(TransactionSheet.Category)) },
                modifier = Modifier.weight(CATEGORY_CHIP_WEIGHT),
            )
        }

        FormMetaRow(
            dateLabel = state.dateLabel,
            note = state.description,
            onDateClick = { onIntent(EditTransactionIntent.OnSheetRequested(TransactionSheet.Date)) },
            onNoteClick = { onIntent(EditTransactionIntent.OnSheetRequested(TransactionSheet.Note)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s6),
        )

        Spacer(Modifier.weight(1f))

        Numpad(
            onDigit = { digit ->
                val newAmount = (state.amount + digit).take(9)
                onIntent(EditTransactionIntent.OnAmountChange(newAmount))
            },
            onDoubleZero = {
                val newAmount = (state.amount + "00").take(9)
                onIntent(EditTransactionIntent.OnAmountChange(newAmount))
            },
            onBackspace = {
                val newAmount = state.amount.dropLast(1)
                onIntent(EditTransactionIntent.OnAmountChange(newAmount))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4)
                .padding(bottom = spacing.s2),
        )

        StickyCTA(
            label = "Guardar cambios",
            tone = CtaTone.Accent,
            interaction = if (state.isEnabled) CtaInteraction.Enabled else CtaInteraction.Disabled,
            onClick = { onIntent(EditTransactionIntent.OnSave) },
        )
    }

    if (state.openSheet == TransactionSheet.Account) {
        AccountPickerSheet(
            accounts = state.accounts,
            selectedAccountId = state.accountSelected?.accountId?.value,
            onSelect = { onIntent(EditTransactionIntent.OnAccountSelected(it)) },
            onDismiss = { onIntent(EditTransactionIntent.OnSheetDismissed) },
            onAddNew = { onAddNewAccount() },
        )
    }

    if (state.openSheet == TransactionSheet.Category) {
        CategoryPickerSheet(
            categories = state.categories,
            selectedCategoryId = state.categorySelected?.categoryId?.value,
            onSelect = { onIntent(EditTransactionIntent.OnCategorySelected(it)) },
            onAddNew = { onIntent(EditTransactionIntent.OnSheetDismissed) },
            onDismiss = { onIntent(EditTransactionIntent.OnSheetDismissed) },
            frequentCategoryIds = state.frequentCategoryIds,
        )
    }

    if (state.openSheet == TransactionSheet.Date) {
        DatePickerSheet(
            currentDate = state.date,
            onConfirm = { date -> onIntent(EditTransactionIntent.OnDateSelected(date)) },
            onDismiss = { onIntent(EditTransactionIntent.OnSheetDismissed) },
        )
    }

    if (state.openSheet == TransactionSheet.Note) {
        NoteSheet(
            initialNote = state.description,
            onSave = { note -> onIntent(EditTransactionIntent.OnDescriptionChange(note)) },
            onDismiss = { onIntent(EditTransactionIntent.OnSheetDismissed) },
        )
    }

    if (showDeleteDialog) {
        DeleteTransactionDialog(
            type = state.transactionType,
            amountCents = state.amount,
            accountName = state.accountSelected?.name,
            categoryName = state.categorySelected?.name,
            categoryColor = state.categorySelected?.resolvedColor?.primary,
            onConfirm = {
                showDeleteDialog = false
                onIntent(EditTransactionIntent.OnDelete)
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Preview
@Composable
private fun EditTransactionPreview() {
    EmmTheme {
        EditTransactionContent(
            state = EditTransactionUiState(
                date = LocalDate(2026, Month.AUGUST, 10),
                today = LocalDate(2026, Month.AUGUST, 10),
                amount = "8540",
                transactionType = TransactionType.Spend,
                description = "Mercado Vea — pollo y verduras",
            ),
            onIntent = {},
            onBack = {},
        )
    }
}
