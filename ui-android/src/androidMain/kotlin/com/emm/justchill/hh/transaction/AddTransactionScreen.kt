package com.emm.justchill.hh.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.Numpad
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.CtaTone
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.account.accountDotColor
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.allColors
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.transaction.components.FrequentComboChip
import com.emm.justchill.hh.transaction.components.NoteRow
import com.emm.justchill.hh.transaction.components.QuickChip
import com.emm.justchill.hh.transaction.components.SignToggle
import com.emm.justchill.hh.transaction.sheets.AccountPickerSheet
import com.emm.justchill.hh.transaction.sheets.CategoryPickerSheet
import com.emm.justchill.hh.transaction.sheets.DatePickerSheet
import com.emm.justchill.hh.transaction.sheets.NoteSheet
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

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

                AddTransactionEffect.FocusAmountField -> Unit // Numpad field; no focus action needed.
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

    var showAccountSheet by rememberSaveable { mutableStateOf(false) }
    var showCategorySheet by rememberSaveable { mutableStateOf(false) }
    var showDateSheet by rememberSaveable { mutableStateOf(false) }
    var showNoteSheet by rememberSaveable { mutableStateOf(false) }

    val isSpend = state.transactionType == TransactionType.Spend
    val noAccounts = state.accounts.isEmpty()

    val ctaAmount = remember(state.amount) {
        "S/ ${formatCentsForDisplay(state.amount)}"
    }
    val ctaLabel = when {
        noAccounts -> "Crea una cuenta primero"
        isSpend -> "Anotar gasto"
        else -> "Anotar ingreso"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        JcTopBar(
            title = if (isSpend) "Nuevo gasto" else "Nuevo ingreso",
            left = {
                IconBtn(
                    icon = Icons.Outlined.Close,
                    onClick = popBackStack,
                    contentDescription = "Cerrar",
                )
            },
            right = null,
        )

        SignToggle(
            isSpend = isSpend,
            onIncomeClick = { onIntent(AddTransactionIntent.OnTransactionTypeChange(TransactionType.Income)) },
            onSpendClick = { onIntent(AddTransactionIntent.OnTransactionTypeChange(TransactionType.Spend)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 2.dp),
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 0.dp)
                .padding(top = 20.dp, bottom = 18.dp),
        ) {
            AmountHero(
                value = centsToSoles(state.amount),
                size = 48.sp,
                tone = if (isSpend) AmountTone.Neg else AmountTone.Pos,
                showCaret = true,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (noAccounts) {
                QuickChip(
                    eyebrow = "CUENTA",
                    value = "Crear cuenta",
                    dotColor = colors.accent,
                    onClick = onAddNewAccount,
                    cta = true,
                    modifier = Modifier.weight(1f),
                )
            } else {
                QuickChip(
                    eyebrow = "CUENTA",
                    value = state.accountSelected?.name ?: "—",
                    dotColor = state.accountSelected?.let {
                        accountDotColor(it.name, colors)
                    },
                    onClick = { showAccountSheet = true },
                    modifier = Modifier.weight(1f),
                )
            }

            QuickChip(
                eyebrow = "CATEGORÍA",
                value = state.categorySelected?.name ?: "—",
                dotColor = state.categorySelected?.resolvedColor?.primary,
                onClick = { showCategorySheet = true },
                modifier = Modifier.weight(1f),
            )

            QuickChip(
                eyebrow = "FECHA",
                value = state.dateLabel,
                dotColor = null,
                onClick = { showDateSheet = true },
                modifier = Modifier.weight(1f),
            )
        }

        NoteRow(
            note = state.description,
            onClick = { showNoteSheet = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )

        if (state.frequentCombos.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                items(state.frequentCombos) { combo ->
                    FrequentComboChip(
                        label = combo.label,
                        dotColor = combo.colorId?.let { findById(it).primary },
                        onClick = { onIntent(AddTransactionIntent.OnFrequentComboSelected(combo)) },
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

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
                .padding(horizontal = 14.dp)
                .padding(bottom = 6.dp),
        )

        StickyCTA(
            label = ctaLabel,
            sublabel = if (noAccounts) null else ctaAmount,
            inlineSublabel = !noAccounts,
            tone = CtaTone.Accent,
            interaction = if (state.isEnabled) CtaInteraction.Enabled else CtaInteraction.Disabled,
            onClick = { onIntent(AddTransactionIntent.OnSave) },
        )
    }

    if (showAccountSheet) {
        AccountPickerSheet(
            accounts = state.accounts,
            selectedAccountId = state.accountSelected?.accountId?.value,
            onSelect = { onIntent(AddTransactionIntent.OnAccountSelected(it)) },
            onDismiss = { showAccountSheet = false },
            onAddNew = { onAddNewAccount() },
        )
    }

    if (showCategorySheet) {
        CategoryPickerSheet(
            categories = state.categories,
            selectedCategoryId = state.categorySelected?.categoryId?.value,
            onSelect = { onIntent(AddTransactionIntent.OnCategorySelected(it)) },
            onAddNew = { onAddNewCategory(state.transactionType.categoryType) },
            onDismiss = { showCategorySheet = false },
            frequentCategoryIds = state.frequentCategoryIds,
        )
    }

    if (showDateSheet) {
        DatePickerSheet(
            currentDate = state.pickerDate,
            onConfirm = { date -> onIntent(AddTransactionIntent.OnDateSelected(date)) },
            onDismiss = { showDateSheet = false },
        )
    }

    if (showNoteSheet) {
        NoteSheet(
            initialNote = state.description,
            onSave = { note -> onIntent(AddTransactionIntent.OnDescriptionChange(note)) },
            onDismiss = { showNoteSheet = false },
        )
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
                categories = categories,
                amount = "8540",
                transactionType = TransactionType.Spend,
            ),
            onIntent = {},
            popBackStack = {},
        )
    }
}
