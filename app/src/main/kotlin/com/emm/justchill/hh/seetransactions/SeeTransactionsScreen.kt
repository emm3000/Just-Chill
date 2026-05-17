package com.emm.justchill.hh.seetransactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.components.EmmListItem
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.transaction.CategoryUi
import com.emm.justchill.hh.transaction.TransactionUi
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.util.UUID

@Composable
fun SeeTransactionsScreen(
    onEditTransaction: (String) -> Unit,
    vm: SeeTransactionsViewModel = koinViewModel(),
) {
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
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        ScreenHeader(title = "Transacciones")

        SearchHeader(
            query = state.query,
            onQueryChange = { onIntent(SeeTransactionsIntent.OnQueryChanged(it)) },
        )

        CategoryChipsRow(
            chips = state.categoryChips,
            onToggle = { onIntent(SeeTransactionsIntent.OnCategoryToggled(it)) },
        )

        if (state.isFilterActive) {
            ActiveFilterBanner(
                onClear = { onIntent(SeeTransactionsIntent.OnClearFilters) },
            )
        }

        when {
            state.hasNoTransactionsAtAll -> EmptyState(modifier = Modifier.fillMaxSize())
            state.hasNoResultsForFilter -> NoResultsState(
                query = state.query,
                onClear = { onIntent(SeeTransactionsIntent.OnClearFilters) },
                modifier = Modifier.fillMaxSize(),
            )
            else -> TransactionList(
                days = state.days,
                onItemClick = navigateToEdit,
            )
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val spacing = LocalEmmSpacing.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Limpiar")
                }
            }
        },
        placeholder = { Text("Buscar por descripción") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4),
    )
}

@Composable
private fun CategoryChipsRow(
    chips: List<CategoryChipUi>,
    onToggle: (String) -> Unit,
) {
    if (chips.isEmpty()) return

    val spacing = LocalEmmSpacing.current

    LazyRow(
        contentPadding = PaddingValues(horizontal = spacing.s4),
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.s2),
    ) {
        items(chips, key = { it.id }) { chip ->
            FilterChip(
                selected = chip.selected,
                onClick = { onToggle(chip.id) },
                label = { Text(chip.name) },
            )
        }
    }
}

@Composable
private fun ActiveFilterBanner(onClear: () -> Unit) {
    val spacing = LocalEmmSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onClear) {
            Text("Limpiar filtros")
        }
    }
}

@Composable
private fun NoResultsState(
    query: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier.padding(horizontal = spacing.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(spacing.s4))
        Text(
            text = if (query.isBlank()) "Sin resultados" else "Sin resultados para «$query»",
            style = type.headlineM,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.s3))
        TextButton(onClick = onClear) {
            Text("Limpiar filtros")
        }
    }
}

@Composable
private fun TransactionList(
    days: List<DayGroup>,
    onItemClick: (String) -> Unit,
) {
    val type = LocalEmmType.current
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    val listState = rememberLazyListState()

    LaunchedEffect(days.size) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = spacing.s8),
    ) {
        days.forEach { dayGroup ->
            item(key = "header-${dayGroup.date}") {
                Text(
                    text = dayGroup.readableDate,
                    style = type.labelM,
                    color = colors.textTertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = spacing.s4,
                            end = spacing.s4,
                            top = spacing.s5,
                            bottom = spacing.s2,
                        ),
                )
            }
            items(dayGroup.transactions, key = TransactionUi::transactionId) { tx ->
                EmmListItem(
                    icon = tx.category.categoryIcon,
                    title = tx.description.ifBlank { "Sin descripción" },
                    metadata = tx.readableTime,
                    amount = tx.amount,
                    categoryColor = tx.category.categoryColor.primary,
                    onClick = { onItemClick(tx.transactionId) },
                )
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = spacing.s4,
                end = spacing.s4,
                top = spacing.s6,
                bottom = spacing.s4,
            ),
    ) {
        Text(
            text = title,
            style = type.headlineL,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier.padding(horizontal = spacing.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Receipt,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(spacing.s4))
        Text(
            text = "Aún sin transacciones",
            style = type.headlineM,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(spacing.s2))
        Text(
            text = "Las que registres aparecerán aquí",
            style = type.bodyM,
            color = colors.textSecondary,
        )
    }
}

@PreviewLightDark
@Composable
private fun SeeTransactionsEmptyPreview() {
    EmmTheme {
        SeeTransactionsContent(
            state = SeeTransactionsUiState(),
            onIntent = {},
            navigateToEdit = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SeeTransactionsPopulatedPreview() {
    EmmTheme {
        val txs: List<TransactionUi> = remember {
            listOf(
                TransactionUi(
                    transactionId = UUID.randomUUID().toString(),
                    type = TransactionType.Spend,
                    amount = formatExpense("84.20"),
                    description = "Mercado del lunes",
                    date = 0,
                    readableDate = "HOY",
                    readableTime = "14:30",
                    category = CategoryUi(Icons.Rounded.Category, findById("green")),
                ),
                TransactionUi(
                    transactionId = UUID.randomUUID().toString(),
                    type = TransactionType.Income,
                    amount = formatIncome("3,200.00"),
                    description = "Sueldo",
                    date = 0,
                    readableDate = "HOY",
                    readableTime = "09:00",
                    category = CategoryUi(Icons.Rounded.Category, findById("gray")),
                ),
                TransactionUi(
                    transactionId = UUID.randomUUID().toString(),
                    type = TransactionType.Spend,
                    amount = formatExpense("12.00"),
                    description = "Café",
                    date = 0,
                    readableDate = "AYER",
                    readableTime = "16:48",
                    category = CategoryUi(Icons.Rounded.Category, findById("pink")),
                ),
            )
        }
        val chips = listOf(
            CategoryChipUi(id = "1", name = "Comida", colorId = "green", selected = false),
            CategoryChipUi(id = "2", name = "Salud", colorId = "blue", selected = false),
        )
        SeeTransactionsContent(
            state = SeeTransactionsUiState(
                days = listOf(
                    DayGroup(LocalDate.now(), txs.take(2)),
                    DayGroup(LocalDate.now().minusDays(1), txs.takeLast(1)),
                ),
                categoryChips = chips,
            ),
            onIntent = {},
            navigateToEdit = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SeeTransactionsNoResultsPreview() {
    EmmTheme {
        SeeTransactionsContent(
            state = SeeTransactionsUiState(
                days = emptyList(),
                query = "café",
                isFilterActive = true,
            ),
            onIntent = {},
            navigateToEdit = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SeeTransactionsWithFiltersPreview() {
    EmmTheme {
        val chips = listOf(
            CategoryChipUi(id = "1", name = "Comida", colorId = "green", selected = true),
            CategoryChipUi(id = "2", name = "Salud", colorId = "blue", selected = false),
            CategoryChipUi(id = "3", name = "Transporte", colorId = "orange", selected = false),
        )
        SeeTransactionsContent(
            state = SeeTransactionsUiState(
                days = emptyList(),
                categoryChips = chips,
                isFilterActive = true,
            ),
            onIntent = {},
            navigateToEdit = {},
        )
    }
}
