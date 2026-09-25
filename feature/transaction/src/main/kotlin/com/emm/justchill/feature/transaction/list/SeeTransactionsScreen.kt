package com.emm.justchill.feature.transaction.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.category.CategoryUi
import com.emm.justchill.core.ui.format.formatExpense
import com.emm.justchill.core.ui.format.formatIncome
import com.emm.justchill.core.ui.format.moneyCentsString
import com.emm.justchill.core.ui.pending.ConfirmRecurringSheet
import com.emm.justchill.core.ui.pending.PendingRecurringHeader
import com.emm.justchill.core.ui.pending.PendingRecurringRow
import com.emm.justchill.core.ui.pending.PendingRecurringUi
import com.emm.justchill.core.ui.sheets.AmountInputSheet
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.InterFontFamily
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import com.emm.justchill.core.ui.transaction.TransactionRow
import com.emm.justchill.core.ui.transaction.TransactionUi
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Composable
fun SeeTransactionsScreen(
    onEditTransaction: (String) -> Unit,
    onAddTransaction: () -> Unit,
    onBack: () -> Unit,
    vm: SeeTransactionsViewModel,
) {
    val state by vm.state.collectAsStateWithLifecycle()

    SeeTransactionsContent(
        state = state,
        onIntent = vm::onIntent,
        navigateToEdit = onEditTransaction,
        navigateToAdd = onAddTransaction,
        onBack = onBack,
    )
}

@Composable
internal fun SeeTransactionsContent(
    state: SeeTransactionsUiState,
    onIntent: (SeeTransactionsIntent) -> Unit,
    navigateToEdit: (String) -> Unit,
    navigateToAdd: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalEmmColors.current

    val pendingMap = remember(state.pendingRecurringMovements) {
        state.pendingRecurringMovements.associateBy { it.id }
    }

    BackHandler(enabled = state.isSearchOpen) { onIntent(SeeTransactionsIntent.ScreenChromeIntent.OnSearchClosed) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        if (state.isSearchOpen) {
            SearchBar(
                query = state.query,
                onQueryChange = { onIntent(SeeTransactionsIntent.OnQueryChanged(it)) },
                onClose = { onIntent(SeeTransactionsIntent.ScreenChromeIntent.OnSearchClosed) },
            )
        } else {
            ScreenHeader(
                month = state.month.takeIf { state.isMonthSelectorVisible },
                isCategoryOrAmountFilterActive = state.isCategoryOrAmountFilterActive,
                onBack = onBack,
                onIntent = onIntent,
            )
        }

        val summary: MonthSummaryUi? = state.summary
            ?.takeIf { state.listDisplayState == ListDisplayState.Content }
        val isMonthNavigationVisible: Boolean = state.isMonthSelectorVisible && !state.isSearchOpen
        if (isMonthNavigationVisible || summary != null) {
            MonthStrip(
                isMonthNavigationVisible = isMonthNavigationVisible,
                summary = summary,
                onIntent = onIntent,
            )
        }

        if (state.isTodayNudgeVisible) {
            TodayNudgeCard(onClick = navigateToAdd)
        }

        if (summary != null || state.isTodayNudgeVisible) {
            Spacer(Modifier.height(LocalEmmSpacing.current.s2))
        }

        val activeCategory: ActiveCategoryInfo? = state.activeCategory
        if (state.isCategoryOrAmountFilterActive) {
            ActiveFilterBanner(
                categoryName = activeCategory?.name,
                minAmount = state.minAmount,
                maxAmount = state.maxAmount,
                query = state.query.takeIf { it.isNotBlank() },
                onClear = { onIntent(SeeTransactionsIntent.OnClearCategoryFilter) },
            )
        }

        TransactionListColumn(
            state = state,
            onIntent = onIntent,
            navigateToEdit = navigateToEdit,
            onPendingClick = { pending -> onIntent(SeeTransactionsIntent.OnPendingClicked(pending.id)) },
        )
    }

    FilterSheets(state = state, onIntent = onIntent)

    PendingConfirmSheetHost(
        pendingItem = state.confirmSheetPendingId?.let(pendingMap::get),
        onIntent = onIntent,
        onDismiss = { onIntent(SeeTransactionsIntent.OnConfirmSheetDismissed) },
    )
}

/**
 * One `LazyColumn` for everything below the filters, pendings included — never gated behind
 * `Content`. `EmptyMonth`/`EmptyLedger` are real states for an account with dues but no bookings
 * yet, and the pending row is often the only door that lets the author create the first one.
 */
@Composable
private fun TransactionListColumn(
    state: SeeTransactionsUiState,
    onIntent: (SeeTransactionsIntent) -> Unit,
    navigateToEdit: (String) -> Unit,
    onPendingClick: (PendingRecurringUi) -> Unit,
) {
    val listState: LazyListState = rememberLazyListState()
    val spacing: EmmSpacing = LocalEmmSpacing.current

    LaunchedEffect(state.days.size) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = spacing.s4),
    ) {
        if (state.isPendingSectionVisible) {
            item {
                PendingRecurringHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = spacing.s6, end = spacing.s6, top = spacing.s4, bottom = spacing.s2),
                )
            }
            items(state.pendingRecurringMovements, PendingRecurringUi::id) { pendingItem ->
                PendingRecurringRow(item = pendingItem, onClick = { onPendingClick(pendingItem) })
            }
        }

        // fillParentMaxHeight() sets height only, not width — these wrap-content Columns need
        // fillMaxWidth() explicitly, or their CenterHorizontally hugs the left edge instead.
        // It's a LazyItemScope extension, so it can only be built inside an `item` lambda.
        val emptyStateModifier: LazyItemScope.() -> Modifier = {
            if (state.isPendingSectionVisible) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.fillMaxWidth().fillParentMaxHeight()
            }
        }

        when (state.listDisplayState) {
            ListDisplayState.Loading -> item { Spacer(emptyStateModifier()) }

            ListDisplayState.EmptyLedger -> item {
                EmptyNoTransactionsAtAll(modifier = emptyStateModifier())
            }

            ListDisplayState.EmptyMonth -> item {
                EmptyMonth(modifier = emptyStateModifier())
            }

            ListDisplayState.NoSearchResults -> item {
                EmptyFilteredNoResults(
                    query = state.query,
                    activeCategoryName = state.activeCategory?.name,
                    onClear = { onIntent(SeeTransactionsIntent.OnClearFilters) },
                    modifier = emptyStateModifier(),
                )
            }

            ListDisplayState.Content -> dayGroupedItems(
                days = state.days,
                showMonthYearCaption = state.isFilterActive,
                onItemClick = navigateToEdit,
            )
        }
    }
}

@Composable
private fun FilterSheets(state: SeeTransactionsUiState, onIntent: (SeeTransactionsIntent) -> Unit) {
    if (state.showFilterSheet) {
        CategoryFilterSheet(
            items = state.sheetItems,
            incomeCount = state.incomeCount,
            spendCount = state.spendCount,
            hasActiveFilter = state.isCategoryOrAmountFilterActive,
            initialSegment = state.sheetItems
                .firstOrNull { it.id == state.activeCategory?.id }
                ?.type
                ?: CategoryType.Spend,
            minAmount = state.minAmount,
            maxAmount = state.maxAmount,
            onSelect = { onIntent(SeeTransactionsIntent.OnCategorySelected(it)) },
            onClear = { onIntent(SeeTransactionsIntent.OnClearCategoryFilter) },
            onAmountBoundClick = {
                onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountSheetRequested(it))
            },
            onAmountBoundClear = {
                onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountBoundCleared(it))
            },
            onDismiss = { onIntent(SeeTransactionsIntent.ScreenChromeIntent.OnFilterSheetDismissed) },
        )
    }

    val amountSheetTarget: AmountRangeTarget? = state.amountSheetTarget
    if (amountSheetTarget != null) {
        val currentAmount: Money? = when (amountSheetTarget) {
            AmountRangeTarget.Min -> state.minAmount
            AmountRangeTarget.Max -> state.maxAmount
        }
        AmountInputSheet(
            amountDigits = currentAmount?.let { moneyCentsString(it) } ?: "",
            title = when (amountSheetTarget) {
                AmountRangeTarget.Min -> "Monto mínimo"
                AmountRangeTarget.Max -> "Monto máximo"
            },
            tone = AmountTone.Neutral,
            onAmountConfirm = {
                onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountConfirmed(it))
            },
            onDismiss = { onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountSheetDismissed) },
        )
    }
}

@Composable
private fun PendingConfirmSheetHost(
    pendingItem: PendingRecurringUi?,
    onIntent: (SeeTransactionsIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    if (pendingItem == null) return

    ConfirmRecurringSheet(
        item = pendingItem,
        onConfirm = { callerAmount ->
            onIntent(
                SeeTransactionsIntent.ConfirmRecurring(
                    templateId = pendingItem.templateId,
                    period = pendingItem.period,
                    callerAmount = callerAmount,
                ),
            )
        },
        onSkip = {
            onIntent(
                SeeTransactionsIntent.SkipRecurring(templateId = pendingItem.templateId, period = pendingItem.period),
            )
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun ActiveFilterBanner(
    categoryName: String?,
    minAmount: Money?,
    maxAmount: Money?,
    query: String?,
    onClear: () -> Unit,
) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current

    val nbspMin: String? = minAmount?.balanceFormattedNonBreaking()
    val nbspMax: String? = maxAmount?.balanceFormattedNonBreaking()
    val rangeText: String? = when {
        nbspMin != null && nbspMax != null -> "$nbspMin – $nbspMax"
        nbspMin != null -> "desde $nbspMin"
        nbspMax != null -> "hasta $nbspMax"
        else -> null
    }

    val displayText: AnnotatedString = buildAnnotatedString {
        if (categoryName != null) {
            append("Filtrando por «")
            withStyle(SpanStyle(fontWeight = FontWeight.W600)) {
                append(categoryName)
            }
            append("»")
        }
        if (rangeText != null) {
            if (categoryName != null) append(", ")
            withStyle(SpanStyle(fontWeight = FontWeight.W600)) { append(rangeText) }
        }
        if (query != null) {
            append(" + \"")
            withStyle(SpanStyle(fontFamily = InterFontFamily)) { append(query) }
            append("\"")
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s6)
            .padding(bottom = spacing.s3)
            .heightIn(min = spacing.s12)
            .clip(radii.rS)
            .background(colors.surface1)
            .border(spacing.hairline, colors.border, radii.rS)
            .padding(start = spacing.s3),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.List,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(spacing.s3),
        )
        Spacer(Modifier.width(spacing.s2))
        Text(
            text = displayText,
            style = type.labelM,
            color = colors.textPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = spacing.s2),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(spacing.s2))
        val clearInteraction: MutableInteractionSource = remember { MutableInteractionSource() }
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier
                .heightIn(min = spacing.s12)
                .widthIn(min = spacing.s12)
                .clickable(interactionSource = clearInteraction, indication = null, onClick = onClear),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(radii.rXS)
                    .indication(clearInteraction, ripple())
                    .padding(horizontal = spacing.s3),
            ) {
                Text(
                    text = "Limpiar",
                    style = type.labelM,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.width(spacing.s1))
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Limpiar filtro",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(spacing.s3),
                )
            }
        }
    }
}

private fun LazyListScope.dayGroupedItems(
    days: List<DayGroup>,
    showMonthYearCaption: Boolean,
    onItemClick: (String) -> Unit,
) {
    days.forEach { dayGroup ->
        stickyHeader(key = "header-${dayGroup.date}", contentType = "day-header") {
            val colors: EmmColors = LocalEmmColors.current
            val type: EmmType = LocalEmmType.current
            val spacing: EmmSpacing = LocalEmmSpacing.current
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
                    .padding(top = spacing.s4, start = spacing.s6, end = spacing.s6, bottom = spacing.s2),
            ) {
                Eyebrow(text = dayGroup.primaryLabel)
                if (showMonthYearCaption) {
                    Text(
                        text = dayGroup.monthYearCaption,
                        style = type.caption,
                        color = colors.textDisabled,
                    )
                }
            }
        }

        items(
            items = dayGroup.transactions,
            key = TransactionUi::transactionId,
            contentType = { "transaction" },
        ) { tx ->
            TransactionRow(tx = tx, onClick = { onItemClick(tx.transactionId) })
        }
    }
}

@Preview
@Composable
private fun SeeTransactionsEmptyPreview() {
    EmmTheme {
        SeeTransactionsContent(
            state = SeeTransactionsUiState(month = PREVIEW_MONTH, movementCount = 0L),
            onIntent = {},
            navigateToEdit = {},
            navigateToAdd = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun SeeTransactionsMonthPreview() {
    EmmTheme {
        val txs: List<TransactionUi> = remember {
            listOf(
                TransactionUi(
                    transactionId = Uuid.random().toString(),
                    type = TransactionType.Spend,
                    amount = formatExpense("84.20"),
                    description = "Mercado del lunes",
                    occurredAt = PREVIEW_OCCURRED_AT,
                    categoryName = "Supermercado",
                    accountName = "BCP",
                    category = CategoryUi(iconId = null, colorId = "green"),
                ),
                TransactionUi(
                    transactionId = Uuid.random().toString(),
                    type = TransactionType.Income,
                    amount = formatIncome("3,200.00"),
                    description = "Sueldo",
                    occurredAt = PREVIEW_OCCURRED_AT,
                    categoryName = "Ingresos",
                    accountName = "BCP",
                    category = CategoryUi(iconId = null, colorId = "gray"),
                ),
                TransactionUi(
                    transactionId = Uuid.random().toString(),
                    type = TransactionType.Spend,
                    amount = formatExpense("12.00"),
                    description = "",
                    occurredAt = PREVIEW_OCCURRED_AT,
                    categoryName = "Transporte",
                    accountName = "Efectivo",
                    category = CategoryUi(iconId = null, colorId = "blue"),
                ),
            )
        }
        SeeTransactionsContent(
            state = SeeTransactionsUiState(
                month = PREVIEW_MONTH,
                days = listOf(previewDayGroup(txs)),
                summary = MonthSummaryUi(income = Money(320_000L), spend = Money(8_420L)),
                movementCount = 2,
            ),
            onIntent = {},
            navigateToEdit = {},
            navigateToAdd = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun SeeTransactionsPopulatedPreview() {
    EmmTheme {
        val txs: List<TransactionUi> = remember {
            listOf(
                TransactionUi(
                    transactionId = Uuid.random().toString(),
                    type = TransactionType.Spend,
                    amount = formatExpense("84.20"),
                    description = "Mercado del lunes",
                    occurredAt = PREVIEW_OCCURRED_AT,
                    categoryName = "Supermercado",
                    accountName = "BCP",
                    category = CategoryUi(iconId = null, colorId = "green"),
                ),
            )
        }
        SeeTransactionsContent(
            state = SeeTransactionsUiState(
                month = PREVIEW_MONTH,
                days = listOf(previewDayGroup(txs)),
                movementCount = 1,
                activeCategory = ActiveCategoryInfo("4", "Ocio"),
            ),
            onIntent = {},
            navigateToEdit = {},
            navigateToAdd = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun SeeTransactionsWithPendingPreview() {
    EmmTheme {
        val txs: List<TransactionUi> = remember {
            listOf(
                TransactionUi(
                    transactionId = Uuid.random().toString(),
                    type = TransactionType.Spend,
                    amount = formatExpense("84.20"),
                    description = "Mercado del lunes",
                    occurredAt = PREVIEW_OCCURRED_AT,
                    categoryName = "Supermercado",
                    accountName = "BCP",
                    category = CategoryUi(iconId = null, colorId = "green"),
                ),
            )
        }
        val pending = remember {
            listOf(
                PendingRecurringUi(
                    id = "rm-1@2026-08",
                    templateId = "rm-1",
                    period = PREVIEW_MONTH,
                    periodLabel = "Agosto 2026",
                    isCatchUp = false,
                    name = "Netflix",
                    type = TransactionType.Spend,
                    formattedAmount = "-S/ 18.00",
                    isVariableAmount = false,
                    dayOfMonth = 15,
                    accountId = "acc-1",
                    categoryId = null,
                    description = "",
                    fixedAmountCents = 1800L,
                ),
            )
        }
        SeeTransactionsContent(
            state = SeeTransactionsUiState(
                month = PREVIEW_MONTH,
                days = listOf(previewDayGroup(txs)),
                movementCount = 1,
                pendingRecurringMovements = pending,
            ),
            onIntent = {},
            navigateToEdit = {},
            navigateToAdd = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun SeeTransactionsPendingWithEmptyMonthPreview() {
    EmmTheme {
        val pending = remember {
            listOf(
                "Netflix" to "-S/ 18.00",
                "Spotify" to "-S/ 15.00",
                "Gimnasio" to "-S/ 89.00",
                "Internet" to "-S/ 99.00",
                "Seguro" to "-S/ 45.00",
            ).mapIndexed { index, (name, amount) ->
                PendingRecurringUi(
                    id = "rm-$index@2026-08",
                    templateId = "rm-$index",
                    period = PREVIEW_MONTH,
                    periodLabel = "Agosto 2026",
                    isCatchUp = false,
                    name = name,
                    type = TransactionType.Spend,
                    formattedAmount = amount,
                    isVariableAmount = false,
                    dayOfMonth = 15,
                    accountId = "acc-1",
                    categoryId = null,
                    description = "",
                    fixedAmountCents = 1800L,
                )
            }
        }
        SeeTransactionsContent(
            state = SeeTransactionsUiState(
                month = PREVIEW_MONTH,
                days = emptyList(),
                movementCount = 3,
                pendingRecurringMovements = pending,
                today = LocalDate(2026, 8, 10),
            ),
            onIntent = {},
            navigateToEdit = {},
            navigateToAdd = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun SeeTransactionsNoResultsPreview() {
    EmmTheme {
        SeeTransactionsContent(
            state = SeeTransactionsUiState(
                month = PREVIEW_MONTH,
                days = emptyList(),
                movementCount = 5,
                query = "café",
                activeCategory = ActiveCategoryInfo("1", "Comida"),
            ),
            onIntent = {},
            navigateToEdit = {},
            navigateToAdd = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun SeeTransactionsLongMonthPreview() {
    EmmTheme {
        SeeTransactionsContent(
            state = SeeTransactionsUiState(
                month = YearMonth(2026, Month.SEPTEMBER),
                movementCount = 12,
                today = LocalDate(2026, 9, 10),
            ),
            onIntent = {},
            navigateToEdit = {},
            navigateToAdd = {},
            onBack = {},
        )
    }
}

private fun previewDayGroup(transactions: List<TransactionUi>): DayGroup {
    val today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return DayGroup(date = today, today = today, transactions = transactions)
}

private val PREVIEW_OCCURRED_AT = LocalDateTime(2026, 8, 10, 14, 30)

private val PREVIEW_MONTH = YearMonth(2026, Month.AUGUST)
