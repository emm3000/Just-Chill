package com.emm.justchill.hh.seetransactions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.MoneyInline
import com.emm.justchill.core.ui.atoms.MonthSelector
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.shared.monthYearLabel
import com.emm.justchill.hh.transaction.CategoryUi
import com.emm.justchill.hh.transaction.TransactionUi
import com.emm.justchill.hh.transaction.components.TransactionRow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Composable
fun SeeTransactionsScreen(onEditTransaction: (String) -> Unit, vm: SeeTransactionsViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    SeeTransactionsContent(
        state = state,
        onIntent = vm::onIntent,
        navigateToEdit = onEditTransaction,
    )
}

@Composable
private fun SeeTransactionsContent(
    state: SeeTransactionsUiState,
    onIntent: (SeeTransactionsIntent) -> Unit,
    navigateToEdit: (String) -> Unit,
) {
    val colors = LocalEmmColors.current

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var searchRequested by rememberSaveable { mutableStateOf(false) }

    // A query outstanding with the field hidden would filter the list from nowhere — the state
    // survives a tab switch, `searchRequested` does not — so the query itself also opens the bar.
    val isSearchOpen = searchRequested || state.query.isNotBlank()

    fun closeSearch() {
        searchRequested = false
        onIntent(SeeTransactionsIntent.OnQueryChanged(""))
    }

    BackHandler(enabled = isSearchOpen) { closeSearch() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        if (isSearchOpen) {
            SearchBar(
                query = state.query,
                onQueryChange = { onIntent(SeeTransactionsIntent.OnQueryChanged(it)) },
                onClose = { closeSearch() },
            )
        } else {
            ScreenHeader(
                isCategoryFilterActive = state.activeCategory != null,
                onSearch = { searchRequested = true },
                onFilter = { showFilterSheet = true },
            )
        }

        // Month context lives here; an active filter means global results, so it steps aside.
        if (state.isMonthSelectorVisible) {
            MonthSection(
                state = state,
                onPreviousMonth = { onIntent(SeeTransactionsIntent.OnPreviousMonth) },
                onNextMonth = { onIntent(SeeTransactionsIntent.OnNextMonth) },
            )
        }

        val activeCategory = state.activeCategory
        if (activeCategory != null) {
            ActiveFilterBanner(
                categoryName = activeCategory.name,
                query = state.query.takeIf { it.isNotBlank() },
                onClear = { onIntent(SeeTransactionsIntent.OnClearCategoryFilter) },
            )
        }

        Hairline()

        // One value, no precedence here: :presentation owns which of the overlapping empty
        // states wins, so the SwiftUI screen inherits the same answer.
        when (state.listDisplayState) {
            // Nothing is claimed until the ledger count lands — an empty area, never empty copy.
            ListDisplayState.Loading -> Spacer(Modifier.fillMaxSize())

            ListDisplayState.EmptyLedger -> EmptyNoTransactionsAtAll(modifier = Modifier.fillMaxSize())

            ListDisplayState.EmptyMonth -> EmptyMonth(modifier = Modifier.fillMaxSize())

            ListDisplayState.NoSearchResults -> EmptyFilteredNoResults(
                query = state.query,
                activeCategoryName = state.activeCategory?.name,
                onClear = { onIntent(SeeTransactionsIntent.OnClearFilters) },
                modifier = Modifier.fillMaxSize(),
            )

            ListDisplayState.Content -> DayGroupedList(
                days = state.days,
                showMonthYearCaption = state.isFilterActive,
                onItemClick = navigateToEdit,
            )
        }
    }

    if (showFilterSheet) {
        CategoryFilterSheet(
            items = state.sheetItems,
            incomeCount = state.incomeCount,
            spendCount = state.spendCount,
            hasActiveFilter = state.activeCategory != null,
            initialSegment = state.sheetItems
                .firstOrNull { it.id == state.activeCategory?.id }
                ?.type
                ?: CategoryType.Spend,
            onSelect = { onIntent(SeeTransactionsIntent.OnCategorySelected(it)) },
            onClear = { onIntent(SeeTransactionsIntent.OnClearCategoryFilter) },
            onDismiss = { showFilterSheet = false },
        )
    }
}

/**
 * Title plus the two chrome affordances. Search and category filtering used to occupy two
 * permanent bands above the list — a text field nobody types in most sessions, and a horizontally
 * scrolling chip row — which left the rows about 60% of the screen. Both are one tap away now.
 */
@Composable
private fun ScreenHeader(isCategoryFilterActive: Boolean, onSearch: () -> Unit, onFilter: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 24.dp, end = 24.dp, bottom = 14.dp),
    ) {
        Text(
            text = "Transacciones",
            style = type.headlineM.copy(fontSize = 22.sp, letterSpacing = (-0.44).sp),
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        HeaderAction(
            icon = Icons.Outlined.Search,
            contentDescription = "Buscar transacciones",
            onClick = onSearch,
        )
        Spacer(Modifier.width(8.dp))
        HeaderAction(
            icon = Icons.Outlined.FilterList,
            contentDescription = if (isCategoryFilterActive) {
                "Filtrar por categoría, filtro activo"
            } else {
                "Filtrar por categoría"
            },
            onClick = onFilter,
            showBadge = isCategoryFilterActive,
        )
    }
}

/**
 * The header's 44dp tile, matching the Reporte top bar's. The badge is the only signal that a
 * category filter is on while the sheet is closed — [ActiveFilterBanner] says which one.
 */
@Composable
private fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    showBadge: Boolean = false,
) {
    val colors = LocalEmmColors.current
    val shape = RoundedCornerShape(12.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .background(colors.surface1)
            .border(1.dp, colors.border, shape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
        if (showBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 9.dp, end = 9.dp)
                    // The ring keeps the dot legible where it overlaps the icon's strokes.
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(colors.surface1)
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }
    }
}

@Composable
private fun MonthSection(state: SeeTransactionsUiState, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        MonthSelector(
            label = state.month.monthYearLabel(),
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
        )
    }

    val summary = state.summary
    if (summary != null && state.listDisplayState == ListDisplayState.Content) {
        MonthSummaryStrip(summary = summary)
    }
}

@Composable
private fun MonthSummaryStrip(summary: MonthSummaryUi) {
    val colors = LocalEmmColors.current

    val netCents = summary.net.cents
    val netColor = when {
        netCents > 0L -> colors.success
        netCents < 0L -> colors.danger
        else -> colors.textSecondary
    }

    Row(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SummaryCell(
            label = "Ingresos",
            value = summary.income.cents.toDouble() / 100.0,
            valueColor = colors.textPrimary,
        )
        SummaryDivider()
        SummaryCell(
            label = "Gastos",
            value = summary.spend.cents.toDouble() / 100.0,
            valueColor = colors.textSecondary,
        )
        SummaryDivider()
        SummaryCell(
            label = "Balance",
            value = netCents.toDouble() / 100.0,
            valueColor = netColor,
        )
    }
}

@Composable
private fun SummaryCell(label: String, value: Double, valueColor: Color) {
    val colors = LocalEmmColors.current
    Column {
        Eyebrow(text = label, color = colors.textDisabled)
        Spacer(Modifier.height(4.dp))
        MoneyInline(value = value, color = valueColor)
    }
}

@Composable
private fun SummaryDivider() {
    val colors = LocalEmmColors.current
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(colors.border),
    )
}

/**
 * The expanded search field, standing in for the header row while it is open. It takes the header's
 * slot rather than stacking below it so opening search costs no vertical space.
 *
 * [onClose] both clears the query and collapses the bar; the in-field X still only clears the text,
 * so correcting a typo does not throw the user out of search.
 */
@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 24.dp, end = 24.dp, bottom = 14.dp),
    ) {
        SearchInput(
            query = query,
            onQueryChange = onQueryChange,
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        HeaderAction(
            icon = Icons.Outlined.Close,
            contentDescription = "Cerrar búsqueda",
            onClick = onClose,
        )
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = type.labelM.copy(
                color = colors.textPrimary,
                fontSize = 13.sp,
                letterSpacing = 0.sp,
            ),
            cursorBrush = SolidColor(colors.accent),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Buscar por descripción o monto",
                        style = type.labelM.copy(
                            color = colors.textTertiary,
                            fontSize = 13.sp,
                            letterSpacing = 0.sp,
                        ),
                    )
                }
                inner()
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Limpiar búsqueda",
                tint = colors.textTertiary,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun ActiveFilterBanner(categoryName: String, query: String?, onClear: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val shape = RoundedCornerShape(10.dp)

    val displayText = buildAnnotatedString {
        append("Filtrando por «")
        withStyle(SpanStyle(fontWeight = FontWeight.W600)) {
            append(categoryName)
        }
        append("»")
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
            .padding(horizontal = 24.dp)
            .padding(bottom = 12.dp)
            .clip(shape)
            .background(colors.accentMuted)
            .border(1.dp, colors.accent.copy(alpha = 0.2f), shape)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.List,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = displayText,
            style = type.labelM.copy(
                fontSize = 12.sp,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.W500,
            ),
            color = colors.accent,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClear)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = "Limpiar",
                style = type.labelM.copy(
                    fontSize = 12.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.W500,
                ),
                color = colors.accent,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Limpiar filtro",
                tint = colors.accent,
                modifier = Modifier.size(11.dp),
            )
        }
    }
}

@Composable
private fun DayGroupedList(days: List<DayGroup>, showMonthYearCaption: Boolean, onItemClick: (String) -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val listState = rememberLazyListState()

    LaunchedEffect(days.size) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        days.forEach { dayGroup ->
            stickyHeader(key = "header-${dayGroup.date}", contentType = "day-header") {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bg)
                        .padding(top = 14.dp, start = 24.dp, end = 24.dp, bottom = 8.dp),
                ) {
                    Eyebrow(text = dayGroup.primaryLabel)
                    if (showMonthYearCaption) {
                        Text(
                            text = dayGroup.monthYearCaption,
                            style = type.eyebrow.copy(fontSize = 11.sp, letterSpacing = 1.0.sp),
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
                TransactionRow(
                    tx = tx,
                    showDate = false,
                    onClick = { onItemClick(tx.transactionId) },
                )
            }
        }
    }
}

@Composable
private fun EmptyNoTransactionsAtAll(modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface1)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
        ) {
            Icon(
                imageVector = Icons.Outlined.Receipt,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Aún sin transacciones",
            style = type.titleL.copy(fontSize = 15.sp, letterSpacing = (-0.075).sp),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Las que registres aparecerán acá agrupadas por día.",
            style = type.caption.copy(lineHeight = 18.sp),
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 260.dp),
        )
    }
}

@Composable
private fun EmptyMonth(modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Sin movimientos este mes",
            style = type.titleL.copy(fontSize = 15.sp, letterSpacing = (-0.075).sp),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Cambia de mes con las flechas de arriba.",
            style = type.caption.copy(lineHeight = 18.sp),
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp),
        )
    }
}

@Composable
private fun EmptyFilteredNoResults(
    query: String,
    activeCategoryName: String?,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    val headline = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontFamily = type.labelM.fontFamily,
                fontWeight = FontWeight.W600,
                letterSpacing = (-0.15).sp,
            ),
        ) { append("Sin resultados para ") }

        when {
            activeCategoryName != null -> withStyle(
                SpanStyle(
                    color = colors.accent,
                    fontFamily = type.labelM.fontFamily,
                    fontWeight = FontWeight.W600,
                    fontSize = 15.sp,
                    letterSpacing = (-0.15).sp,
                ),
            ) { append("«$activeCategoryName»") }

            query.isNotEmpty() -> withStyle(
                SpanStyle(
                    color = colors.accent,
                    fontFamily = type.labelM.fontFamily,
                    fontWeight = FontWeight.W600,
                    fontSize = 15.sp,
                    letterSpacing = (-0.15).sp,
                ),
            ) { append("«$query»") }
        }
    }

    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (activeCategoryName != null || query.isNotEmpty()) {
            Text(text = headline, textAlign = TextAlign.Center)
        } else {
            Text(
                text = "Sin movimientos con esos filtros",
                style = type.labelM.copy(fontSize = 15.sp, letterSpacing = (-0.15).sp),
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Prueba con otro nombre, otro monto, o limpia los filtros activos.",
            style = type.caption.copy(lineHeight = 18.sp),
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp),
        )
        Spacer(Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, colors.borderFocus, RoundedCornerShape(999.dp))
                .clickable { onClear() }
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(11.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Limpiar filtros",
                style = type.labelM.copy(fontSize = 12.sp, letterSpacing = 0.sp),
                color = colors.textPrimary,
            )
        }
    }
}

@Preview
@Composable
private fun SeeTransactionsEmptyPreview() {
    EmmTheme {
        SeeTransactionsContent(
            // An explicit zero: the default count is null, which is "not known yet", not "empty".
            state = SeeTransactionsUiState(month = PREVIEW_MONTH, movementCount = 0L),
            onIntent = {},
            navigateToEdit = {},
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
                    readableDate = "HOY",
                    readableTime = "14:30",
                    category = CategoryUi(iconId = null, colorId = "green"),
                ),
                TransactionUi(
                    transactionId = Uuid.random().toString(),
                    type = TransactionType.Income,
                    amount = formatIncome("3,200.00"),
                    description = "Sueldo",
                    occurredAt = PREVIEW_OCCURRED_AT,
                    readableDate = "HOY",
                    readableTime = "09:00",
                    category = CategoryUi(iconId = null, colorId = "gray"),
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
                    readableDate = "HOY",
                    readableTime = "14:30",
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
        )
    }
}

private fun previewDayGroup(transactions: List<TransactionUi>): DayGroup {
    val today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return DayGroup(date = today, today = today, transactions = transactions)
}

/** Any fixed local datetime — previews render the pre-formatted labels, never this value. */
private val PREVIEW_OCCURRED_AT = LocalDateTime(2026, 8, 10, 14, 30)

/** Fixed, matching [PREVIEW_OCCURRED_AT]. A preview that read the clock would drift with the day. */
private val PREVIEW_MONTH = YearMonth(2026, Month.AUGUST)
