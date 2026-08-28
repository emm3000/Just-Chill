package com.emm.justchill.hh.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.CategoryId
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
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.account.accountDotColor
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.allColors
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.transaction.components.ACCOUNT_CHIP_WEIGHT
import com.emm.justchill.hh.transaction.components.CATEGORY_CHIP_WEIGHT
import com.emm.justchill.hh.transaction.components.FormMetaRow
import com.emm.justchill.hh.transaction.components.FrequentComboChip
import com.emm.justchill.hh.transaction.components.SelectorChip
import com.emm.justchill.hh.transaction.components.SignToggle
import com.emm.justchill.hh.transaction.sheets.AccountPickerSheet
import com.emm.justchill.hh.transaction.sheets.CategoryPickerSheet
import com.emm.justchill.hh.transaction.sheets.DatePickerSheet
import com.emm.justchill.hh.transaction.sheets.NoteSheet
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

private fun ctaInteraction(state: AddTransactionUiState): CtaInteraction = when {
    state.isSaving -> CtaInteraction.Loading
    state.missingField == null -> CtaInteraction.Enabled
    else -> CtaInteraction.Disabled
}

private data class CtaContent(val label: String, val sublabel: String?)
private data class TransactionKindContent(val amountTone: AmountTone, val ctaLabel: String)

@Composable
fun AddTransactionScreen(
    vm: AddTransactionViewModel,
    popBackStack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onAddNewCategory: (CategoryType) -> Unit = {},
    onAddNewAccount: () -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val currentPopBackStack by rememberUpdatedState(popBackStack)

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                AddTransactionEffect.TransactionSaved -> currentPopBackStack()

                is AddTransactionEffect.ShowError -> snackbarHostState.showEmmSnackbar(
                    message = effect.message,
                    tone = EmmSnackbarTone.Error,
                )
            }
        }
    }

    AddTransactionScreenContent(
        state = state,
        onIntent = vm::onIntent,
        popBackStack = popBackStack,
        onAddNewCategory = onAddNewCategory,
        onAddNewAccount = onAddNewAccount,
    )
}

@Composable
private fun AddTransactionScreenContent(
    state: AddTransactionUiState,
    onIntent: (AddTransactionIntent) -> Unit,
    popBackStack: () -> Unit,
    onAddNewCategory: (CategoryType) -> Unit = {},
    onAddNewAccount: () -> Unit = {},
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    val isSpend = state.transactionType == TransactionType.Spend
    val noAccounts = state.hasNoAccounts

    val ctaAmount = remember(state.amount) {
        "S/ ${formatCentsForDisplay(state.amount)}"
    }
    val kind = if (isSpend) {
        TransactionKindContent(amountTone = AmountTone.Neutral, ctaLabel = "Anotar gasto")
    } else {
        TransactionKindContent(amountTone = AmountTone.Pos, ctaLabel = "Anotar ingreso")
    }
    val cta = if (noAccounts) {
        CtaContent(label = "Crea una cuenta primero", sublabel = null)
    } else {
        CtaContent(label = kind.ctaLabel, sublabel = ctaAmount)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        FormHeader(
            isSpend = isSpend,
            onClose = popBackStack,
            onIncomeClick = { onIntent(AddTransactionIntent.OnTransactionTypeChange(TransactionType.Income)) },
            onSpendClick = { onIntent(AddTransactionIntent.OnTransactionTypeChange(TransactionType.Spend)) },
        )

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
                tone = kind.amountTone,
                showCaret = true,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            if (noAccounts) {
                SelectorChip(
                    label = "Crear cuenta",
                    dotColor = null,
                    onClick = onAddNewAccount,
                    trailingIcon = Icons.Outlined.Add,
                    modifier = Modifier.weight(ACCOUNT_CHIP_WEIGHT),
                )
            } else {
                SelectorChip(
                    label = state.accountSelected?.name ?: "—",
                    dotColor = state.accountSelected?.let { accountDotColor(it.name, colors) },
                    onClick = { onIntent(AddTransactionIntent.OnSheetRequested(TransactionSheet.Account)) },
                    modifier = Modifier.weight(ACCOUNT_CHIP_WEIGHT),
                )
            }

            SelectorChip(
                label = state.categorySelected?.name ?: "—",
                dotColor = state.categorySelected?.resolvedColor?.primary,
                onClick = { onIntent(AddTransactionIntent.OnSheetRequested(TransactionSheet.Category)) },
                modifier = Modifier.weight(CATEGORY_CHIP_WEIGHT),
            )
        }

        FormMetaRow(
            dateLabel = state.dateLabel,
            note = state.description,
            onDateClick = { onIntent(AddTransactionIntent.OnSheetRequested(TransactionSheet.Date)) },
            onNoteClick = { onIntent(AddTransactionIntent.OnSheetRequested(TransactionSheet.Note)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s6),
        )

        if (state.frequentCombos.isEmpty()) {
            Spacer(Modifier.weight(1f))
        } else {
            FrequentCombos(
                combos = state.frequentCombos,
                selectedAccountId = state.accountSelected?.accountId?.value,
                selectedCategoryId = state.categorySelected?.categoryId?.value,
                onSelect = { combo -> onIntent(AddTransactionIntent.OnFrequentComboSelected(combo)) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s4)
                    .padding(top = spacing.s3),
            )
        }

        Numpad(
            onDigit = { digit ->
                val newAmount = (state.amount + digit).take(9)
                onIntent(AddTransactionIntent.OnAmountChange(newAmount))
            },
            onDoubleZero = {
                val newAmount = (state.amount + "00").take(9)
                onIntent(AddTransactionIntent.OnAmountChange(newAmount))
            },
            onBackspace = {
                val newAmount = state.amount.dropLast(1)
                onIntent(AddTransactionIntent.OnAmountChange(newAmount))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4)
                .padding(bottom = spacing.s2),
        )

        StickyCTA(
            label = cta.label,
            sublabel = cta.sublabel,
            inlineSublabel = cta.sublabel != null,
            tone = CtaTone.Accent,
            interaction = ctaInteraction(state),
            onClick = { onIntent(AddTransactionIntent.OnSave) },
        )
    }

    if (state.openSheet == TransactionSheet.Account) {
        AccountPickerSheet(
            accounts = state.accounts,
            selectedAccountId = state.accountSelected?.accountId?.value,
            onSelect = { onIntent(AddTransactionIntent.OnAccountSelected(it)) },
            onDismiss = { onIntent(AddTransactionIntent.OnSheetDismissed) },
            onAddNew = { onAddNewAccount() },
        )
    }

    if (state.openSheet == TransactionSheet.Category) {
        CategoryPickerSheet(
            categories = state.categories,
            selectedCategoryId = state.categorySelected?.categoryId?.value,
            onSelect = { onIntent(AddTransactionIntent.OnCategorySelected(it)) },
            onAddNew = { onAddNewCategory(state.transactionType.categoryType) },
            onDismiss = { onIntent(AddTransactionIntent.OnSheetDismissed) },
            frequentCategoryIds = state.frequentCategoryIds,
        )
    }

    if (state.openSheet == TransactionSheet.Date) {
        DatePickerSheet(
            currentDate = state.pickerDate,
            onConfirm = { date -> onIntent(AddTransactionIntent.OnDateSelected(date)) },
            onDismiss = { onIntent(AddTransactionIntent.OnSheetDismissed) },
        )
    }

    if (state.openSheet == TransactionSheet.Note) {
        NoteSheet(
            initialNote = state.description,
            onSave = { note -> onIntent(AddTransactionIntent.OnDescriptionChange(note)) },
            onDismiss = { onIntent(AddTransactionIntent.OnSheetDismissed) },
        )
    }
}

@Composable
private fun FormHeader(isSpend: Boolean, onClose: () -> Unit, onIncomeClick: () -> Unit, onSpendClick: () -> Unit) {
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconBtn(icon = Icons.Outlined.Close, onClick = onClose, contentDescription = "Cerrar")
        SignToggle(isSpend = isSpend, onIncomeClick = onIncomeClick, onSpendClick = onSpendClick)
        // The pill is centred by what balances the close button, so the empty side keeps its width.
        Spacer(Modifier.size(spacing.s12))
    }
}

@Composable
private fun FrequentCombos(
    combos: List<FrequentComboUi>,
    selectedAccountId: String?,
    selectedCategoryId: String?,
    onSelect: (FrequentComboUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalEmmSpacing.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.s1)) {
        Eyebrow(text = "Tus combinaciones frecuentes", modifier = Modifier.padding(start = spacing.s2))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            verticalArrangement = Arrangement.spacedBy(spacing.s1),
        ) {
            combos.forEach { combo ->
                FrequentComboChip(
                    label = combo.label,
                    dotColor = combo.colorId?.let { findById(it).primary },
                    onClick = { onSelect(combo) },
                    active = combo.accountId == selectedAccountId && combo.categoryId == selectedCategoryId,
                )
            }
        }
    }
}

@Preview
@Composable
private fun AddTransactionPreview() {
    EmmTheme {
        val categories = remember {
            buildList {
                repeat(5) {
                    add(
                        SelectableCategory(
                            categoryId = CategoryId("$it"),
                            name = "Categoría $it",
                            iconId = AppIconCatalog.catalog[it].id,
                            colorId = allColors[it].id,
                            categoryType = CategoryType.Income,
                        ),
                    )
                }
            }
        }
        AddTransactionScreenContent(
            state = AddTransactionUiState(
                today = LocalDate(2026, Month.AUGUST, 10),
                catalog = Catalog.Loaded(
                    accounts = emptyList(),
                    categories = categories.groupBy(SelectableCategory::categoryType),
                ),
                amount = "8540",
                transactionType = TransactionType.Spend,
            ),
            onIntent = {},
            popBackStack = {},
        )
    }
}
