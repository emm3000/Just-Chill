package com.emm.justchill.feature.transaction.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.Numpad
import com.emm.justchill.core.ui.NumpadKeyHeight
import com.emm.justchill.core.ui.NumpadSign
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.core.ui.category.AppIconCatalog
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.category.selectableColorIds
import com.emm.justchill.core.ui.format.MAX_AMOUNT_DIGITS
import com.emm.justchill.core.ui.format.balanceFormatted
import com.emm.justchill.core.ui.format.centsToMoney
import com.emm.justchill.core.ui.format.centsToSoles
import com.emm.justchill.core.ui.format.moneyCentsString
import com.emm.justchill.core.ui.format.positiveMoneyFormatted
import com.emm.justchill.core.ui.preview.PreviewRedmi15C
import com.emm.justchill.core.ui.preview.PreviewWindowEdges
import com.emm.justchill.core.ui.sheets.AccountPickerSheet
import com.emm.justchill.core.ui.sheets.CategoryPickerSheet
import com.emm.justchill.core.ui.sheets.DatePickerSheet
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import com.emm.justchill.core.ui.transaction.Catalog
import com.emm.justchill.feature.transaction.capture.components.CapturePadLayout
import com.emm.justchill.feature.transaction.capture.components.MonthSpendLine
import com.emm.justchill.feature.transaction.capture.components.PadArrangement
import com.emm.justchill.feature.transaction.capture.components.PadForm
import com.emm.justchill.feature.transaction.capture.components.SaveMotion
import com.emm.justchill.feature.transaction.capture.components.isStackedTall
import com.emm.justchill.feature.transaction.capture.components.rememberSaveMotion
import com.emm.justchill.feature.transaction.capture.sheets.NoteSheet
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
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
    onOpenTransactions: () -> Unit,
    onAddNewCategory: (CategoryType) -> Unit = {},
    onAddNewAccount: () -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val currentPopBackStack by rememberUpdatedState(popBackStack)
    val motion: SaveMotion = rememberSaveMotion()

    LaunchedEffect(vm) {
        var flight: Job? = null
        vm.effect.collect { effect ->
            when (effect) {
                is AddTransactionEffect.TransactionSaved -> {
                    currentPopBackStack()
                    flight?.cancelAndJoin()
                    flight = launch { motion.fly(effect.amount) }
                }

                is AddTransactionEffect.ShowError -> {
                    motion.releaseTotal()
                    snackbarHostState.showEmmSnackbar(message = effect.message, tone = EmmSnackbarTone.Error)
                }
            }
        }
    }

    AddTransactionScreenContent(
        state = state,
        onIntent = vm::onIntent,
        onSave = {
            motion.holdTotal(state.monthSpendAmount)
            vm.onIntent(AddTransactionIntent.OnSave)
        },
        motion = motion,
        onOpenMenu = onOpenMenu,
        onOpenTransactions = onOpenTransactions,
        onAddNewCategory = onAddNewCategory,
        onAddNewAccount = onAddNewAccount,
    )
}

@Composable
internal fun AddTransactionScreenContent(
    state: AddTransactionUiState,
    onIntent: (AddTransactionIntent) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenTransactions: () -> Unit,
    onSave: () -> Unit,
    motion: SaveMotion = rememberSaveMotion(),
    onAddNewCategory: (CategoryType) -> Unit = {},
    onAddNewAccount: () -> Unit = {},
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    val isSpend: Boolean = state.transactionType == TransactionType.Spend
    val noAccounts: Boolean = state.hasNoAccounts

    val kind: TransactionKindContent = if (isSpend) SPEND_KIND else INCOME_KIND
    val ctaLabel: String = if (noAccounts) "Crea una cuenta primero" else kind.ctaLabel
    val amountDescription: String = remember(state.amount, kind) {
        kind.describeAmount(centsToMoney(state.amount))
    }

    CapturePadLayout(
        menu = { IconBtn(icon = Icons.Outlined.Menu, onClick = onOpenMenu, contentDescription = "Abrir el menú") },
        monthLine = { lineModifier ->
            MonthSpendLine(
                label = state.monthSpendLabel,
                amount = motion.displayedTotal(state.monthSpendAmount),
                onClick = onOpenTransactions,
                modifier = with(motion) { lineModifier.monthLineTarget() },
            )
        },
        hero = { heroModifier ->
            PadHero(
                amount = state.amount,
                kind = kind,
                motion = motion,
                modifier = heroModifier
                    .padding(horizontal = spacing.s6)
                    .clearAndSetSemantics { contentDescription = amountDescription },
            )
        },
        form = { arrangement ->
            PadForm(
                state = state,
                onIntent = onIntent,
                onAddNewAccount = onAddNewAccount,
                arrangement = arrangement,
            )
        },
        numpad = { arrangement, numpadModifier ->
            PadNumpad(
                amount = state.amount,
                kind = kind,
                onIntent = onIntent,
                arrangement = arrangement,
                modifier = numpadModifier,
            )
        },
        cta = { StickyCTA(label = ctaLabel, interaction = ctaInteraction(state), onClick = onSave) },
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    )

    OpenSheet(
        state = state,
        onIntent = onIntent,
        onAddNewCategory = onAddNewCategory,
        onAddNewAccount = onAddNewAccount,
    )
}

@Composable
private fun OpenSheet(
    state: AddTransactionUiState,
    onIntent: (AddTransactionIntent) -> Unit,
    onAddNewCategory: (CategoryType) -> Unit,
    onAddNewAccount: () -> Unit,
) {
    when (state.openSheet) {
        TransactionSheet.Account -> AccountPickerSheet(
            accounts = state.accounts,
            selectedAccountId = state.accountSelected?.accountId?.value,
            onSelect = { onIntent(AddTransactionIntent.OnAccountSelected(it)) },
            onDismiss = { onIntent(AddTransactionIntent.OnSheetDismissed) },
            onAddNew = { onAddNewAccount() },
        )

        TransactionSheet.Category -> CategoryPickerSheet(
            categories = state.categories,
            selectedCategoryId = state.categorySelected?.categoryId?.value,
            onSelect = { onIntent(AddTransactionIntent.OnCategorySelected(it)) },
            onAddNew = { onAddNewCategory(state.transactionType.categoryType) },
            onDismiss = { onIntent(AddTransactionIntent.OnSheetDismissed) },
            frequentCategoryIds = state.frequentCategoryIds,
        )

        TransactionSheet.Date -> DatePickerSheet(
            currentDate = state.pickerDate,
            onConfirm = { date -> onIntent(AddTransactionIntent.OnDateSelected(date)) },
            onDismiss = { onIntent(AddTransactionIntent.OnSheetDismissed) },
        )

        TransactionSheet.Note -> NoteSheet(
            initialNote = state.description,
            onSave = { note -> onIntent(AddTransactionIntent.OnDescriptionChange(note)) },
            onDismiss = { onIntent(AddTransactionIntent.OnSheetDismissed) },
        )

        null -> Unit
    }
}

@Composable
private fun PadHero(amount: String, kind: TransactionKindContent, motion: SaveMotion, modifier: Modifier = Modifier) {
    val type: EmmType = LocalEmmType.current

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        AmountHero(
            value = centsToSoles(amount),
            size = type.amountHero.fontSize,
            tone = kind.amountTone,
            showCaret = true,
            signed = true,
            modifier = with(motion) { Modifier.restingHero() },
        )
        motion.flyingAmount?.let { saved ->
            AmountHero(
                value = centsToSoles(moneyCentsString(saved)),
                size = type.amountHero.fontSize,
                tone = kind.amountTone,
                signed = true,
                modifier = with(motion) { Modifier.inFlight() },
            )
        }
    }
}

@Composable
private fun PadNumpad(
    amount: String,
    kind: TransactionKindContent,
    onIntent: (AddTransactionIntent) -> Unit,
    arrangement: PadArrangement,
    modifier: Modifier = Modifier,
) {
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val isStackedTall: Boolean = arrangement.isStackedTall
    val onAmountChange: (String) -> Unit = { newAmount ->
        onIntent(AddTransactionIntent.OnAmountChange(newAmount.take(MAX_AMOUNT_DIGITS)))
    }

    Numpad(
        onDigit = { digit -> onAmountChange(amount + digit) },
        onDoubleZero = { onAmountChange(amount + "00") },
        onBackspace = { onAmountChange(amount.dropLast(1)) },
        sign = NumpadSign(
            tone = kind.amountTone,
            contentDescription = kind.signDescription,
            onClick = { onIntent(AddTransactionIntent.OnTransactionTypeChange(kind.toggledType)) },
        ),
        keyHeight = if (isStackedTall) NumpadKeyHeight else spacing.s12,
        keyGap = if (isStackedTall) spacing.s2 else spacing.s1,
        modifier = modifier
            .padding(horizontal = spacing.s4)
            .padding(bottom = if (isStackedTall) spacing.s2 else spacing.s0),
    )
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
                            colorId = selectableColorIds[it],
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
            onOpenTransactions = {},
            onSave = {},
        )
    }
}

@PreviewRedmi15C
@PreviewWindowEdges
@Composable
private fun AddTransactionPopulatedPreview() {
    EmmTheme {
        AddTransactionScreenContent(
            state = populatedCaptureState(),
            onIntent = {},
            onOpenMenu = {},
            onOpenTransactions = {},
            onSave = {},
        )
    }
}
