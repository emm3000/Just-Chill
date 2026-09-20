package com.emm.justchill.feature.transaction.capture

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.Numpad
import com.emm.justchill.core.ui.NumpadSign
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.FrequentComboChip
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.SelectorChip
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.core.ui.category.AppIconCatalog
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.category.allColors
import com.emm.justchill.core.ui.category.findById
import com.emm.justchill.core.ui.category.resolvedColor
import com.emm.justchill.core.ui.format.balanceFormatted
import com.emm.justchill.core.ui.format.centsToMoney
import com.emm.justchill.core.ui.format.centsToSoles
import com.emm.justchill.core.ui.format.positiveMoneyFormatted
import com.emm.justchill.core.ui.sheets.AccountPickerSheet
import com.emm.justchill.core.ui.sheets.CategoryPickerSheet
import com.emm.justchill.core.ui.sheets.DatePickerSheet
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import com.emm.justchill.core.ui.transaction.Catalog
import com.emm.justchill.feature.transaction.capture.components.ACCOUNT_CHIP_WEIGHT
import com.emm.justchill.feature.transaction.capture.components.CATEGORY_CHIP_WEIGHT
import com.emm.justchill.feature.transaction.capture.components.FormMetaRow
import com.emm.justchill.feature.transaction.capture.sheets.NoteSheet
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

private fun ctaInteraction(state: AddTransactionUiState): CtaInteraction = when {
    state.isSaving -> CtaInteraction.Loading
    state.missingField == null -> CtaInteraction.Enabled
    else -> CtaInteraction.Disabled
}

private data class TransactionKindContent(
    val amountTone: AmountTone,
    val ctaLabel: String,
    val signDescription: String,
    val toggledType: TransactionType,
    val describeAmount: (Money) -> String,
)

private val SPEND_KIND = TransactionKindContent(
    amountTone = AmountTone.Neutral,
    ctaLabel = "Anotar gasto",
    signDescription = "Cambiar a ingreso",
    toggledType = TransactionType.Income,
    describeAmount = { money -> "Gasto de ${money.balanceFormatted()}" },
)

private val INCOME_KIND = TransactionKindContent(
    amountTone = AmountTone.Pos,
    ctaLabel = "Anotar ingreso",
    signDescription = "Cambiar a gasto",
    toggledType = TransactionType.Spend,
    describeAmount = { money -> "Ingreso de ${money.positiveMoneyFormatted()}" },
)

@Composable
fun AddTransactionScreen(
    vm: AddTransactionViewModel,
    popBackStack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onOpenMenu: () -> Unit,
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
        onOpenMenu = onOpenMenu,
        onAddNewCategory = onAddNewCategory,
        onAddNewAccount = onAddNewAccount,
    )
}

@Composable
private fun AddTransactionScreenContent(
    state: AddTransactionUiState,
    onIntent: (AddTransactionIntent) -> Unit,
    onOpenMenu: () -> Unit,
    onAddNewCategory: (CategoryType) -> Unit = {},
    onAddNewAccount: () -> Unit = {},
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    val isSpend = state.transactionType == TransactionType.Spend
    val noAccounts = state.hasNoAccounts

    val kind: TransactionKindContent = if (isSpend) SPEND_KIND else INCOME_KIND
    val ctaLabel: String = if (noAccounts) "Crea una cuenta primero" else kind.ctaLabel
    val amountDescription: String = remember(state.amount, kind) {
        kind.describeAmount(centsToMoney(state.amount))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        FormHeader(onOpenMenu = onOpenMenu)

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s6)
                .padding(top = spacing.s8, bottom = spacing.s6)
                .clearAndSetSemantics { contentDescription = amountDescription },
        ) {
            AmountHero(
                value = centsToSoles(state.amount),
                size = type.amountHero.fontSize,
                tone = kind.amountTone,
                showCaret = true,
                signed = true,
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
                    onClickLabel = "Crear una cuenta",
                    onClick = onAddNewAccount,
                    trailingIcon = Icons.Outlined.Add,
                    modifier = Modifier.weight(ACCOUNT_CHIP_WEIGHT),
                )
            } else {
                SelectorChip(
                    label = state.accountSelected?.name ?: "—",
                    dotColor = null,
                    onClickLabel = "Cambiar la cuenta",
                    onClick = { onIntent(AddTransactionIntent.OnSheetRequested(TransactionSheet.Account)) },
                    modifier = Modifier.weight(ACCOUNT_CHIP_WEIGHT),
                )
            }

            SelectorChip(
                label = state.categorySelected?.name ?: "—",
                dotColor = state.categorySelected?.resolvedColor?.primary,
                onClickLabel = "Cambiar la categoría",
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
            sign = NumpadSign(
                tone = kind.amountTone,
                contentDescription = kind.signDescription,
                onClick = { onIntent(AddTransactionIntent.OnTransactionTypeChange(kind.toggledType)) },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4)
                .padding(bottom = spacing.s2),
        )

        StickyCTA(
            label = ctaLabel,
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
private fun FormHeader(onOpenMenu: () -> Unit) {
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBtn(icon = Icons.Outlined.Menu, onClick = onOpenMenu, contentDescription = "Abrir el menú")
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
            onOpenMenu = {},
        )
    }
}
