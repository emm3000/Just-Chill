package com.emm.justchill.hh.seetransactions

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.theme.PlexMonoFontFamily
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.IconTileTone
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.transaction.CategoryUi
import com.emm.justchill.hh.transaction.TransactionUi
import androidx.compose.material.icons.rounded.Category
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        // ── Header ────────────────────────────────────────────────
        ScreenHeader()

        // ── Search ────────────────────────────────────────────────
        SearchInput(
            query = state.query,
            onQueryChange = { onIntent(SeeTransactionsIntent.OnQueryChanged(it)) },
        )

        // ── Category chips ────────────────────────────────────────
        CategoryChipsRow(
            chips = state.categoryChips,
            onToggle = { onIntent(SeeTransactionsIntent.OnCategoryToggled(it)) },
            onClearFilters = { onIntent(SeeTransactionsIntent.OnClearFilters) },
        )

        // ── Hairline ──────────────────────────────────────────────
        Hairline()

        // ── Content area (flex 1) ─────────────────────────────────
        when {
            state.hasNoTransactionsAtAll -> EmptyNoTransactionsAtAll(modifier = Modifier.fillMaxSize())
            state.hasNoResultsForFilter -> EmptyFilteredNoResults(
                query = state.query,
                onClear = { onIntent(SeeTransactionsIntent.OnClearFilters) },
                modifier = Modifier.fillMaxSize(),
            )
            else -> DayGroupedList(
                days = state.days,
                onItemClick = navigateToEdit,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SCREEN HEADER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ScreenHeader() {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Text(
        text = "Transacciones",
        style = type.headlineM.copy(fontSize = 22.sp, letterSpacing = (-0.44).sp),
        color = colors.textPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, start = 24.dp, end = 24.dp, bottom = 12.dp),
    )
}

// ═══════════════════════════════════════════════════════════════
// SEARCH INPUT
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 0.dp)
            .padding(bottom = 14.dp)
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
            modifier = Modifier.weight(1f),
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

// ═══════════════════════════════════════════════════════════════
// CATEGORY CHIPS ROW
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CategoryChipsRow(
    chips: List<CategoryChipUi>,
    onToggle: (String) -> Unit,
    onClearFilters: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    val anySelected = chips.any { it.selected }
    // Synthetic "Todas" chip — active when no category chip is selected
    val todasActive = !anySelected

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
    ) {
        // "Todas" synthetic chip
        item(key = "todas") {
            val bgColor = if (todasActive) colors.textPrimary else colors.bg
            val borderColor = if (todasActive) colors.textPrimary else colors.border
            val textColor = if (todasActive) colors.bg else colors.textSecondary

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(999.dp))
                    .clickable { onClearFilters() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "Todas",
                    style = type.labelM.copy(
                        fontSize = 12.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = textColor,
                )
            }
        }

        items(chips, key = { it.id }) { chip ->
            val bgColor = if (chip.selected) colors.textPrimary else colors.bg
            val borderColor = if (chip.selected) colors.textPrimary else colors.border
            val textColor = if (chip.selected) colors.bg else colors.textSecondary

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(999.dp))
                    .clickable { onToggle(chip.id) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = chip.name,
                    style = type.labelM.copy(
                        fontSize = 12.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = textColor,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// DAY-GROUPED LIST
// ═══════════════════════════════════════════════════════════════

private fun monthShortEs(month: java.time.Month): String = when (month) {
    java.time.Month.JANUARY -> "enero"
    java.time.Month.FEBRUARY -> "febrero"
    java.time.Month.MARCH -> "marzo"
    java.time.Month.APRIL -> "abril"
    java.time.Month.MAY -> "mayo"
    java.time.Month.JUNE -> "junio"
    java.time.Month.JULY -> "julio"
    java.time.Month.AUGUST -> "agosto"
    java.time.Month.SEPTEMBER -> "septiembre"
    java.time.Month.OCTOBER -> "octubre"
    java.time.Month.NOVEMBER -> "noviembre"
    java.time.Month.DECEMBER -> "diciembre"
}

@Composable
private fun DayGroupedList(
    days: List<DayGroup>,
    onItemClick: (String) -> Unit,
) {
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
            // Day label: HOY / AYER from readableDate, else day-name uppercased
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            val dayLabel = when (dayGroup.date) {
                today -> "HOY"
                yesterday -> "AYER"
                else -> dayGroup.date.dayOfWeek
                    .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("es"))
                    .uppercase()
            }
            val dateCaption = "${dayGroup.date.dayOfMonth} ${monthShortEs(dayGroup.date.month)}"

            item(key = "header-${dayGroup.date}") {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, start = 24.dp, end = 24.dp, bottom = 8.dp),
                ) {
                    Eyebrow(text = dayLabel)
                    Text(
                        text = dateCaption,
                        style = type.eyebrow.copy(
                            fontSize = 10.sp,
                            letterSpacing = 0.4.sp,
                        ),
                        color = colors.textDisabled,
                    )
                }
            }

            items(dayGroup.transactions, key = TransactionUi::transactionId) { tx ->
                TxRow(
                    tx = tx,
                    onClick = { onItemClick(tx.transactionId) },
                )
            }
        }
    }
}

@Composable
private fun TxRow(
    tx: TransactionUi,
    onClick: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    val amountColor = if (tx.type == TransactionType.Income) colors.success else colors.textSecondary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 24.dp),
    ) {
        IconTile(
            icon = tx.category.categoryIcon,
            size = IconTileSize.Sm,
            tone = IconTileTone.Swatch,
            swatch = tx.category.categoryColor.primary,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description.ifBlank { "Sin descripción" },
                style = type.labelM.copy(
                    fontSize = 13.sp,
                    letterSpacing = (-0.065).sp,
                ),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = tx.readableTime,
                style = type.caption.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.11.sp,
                ),
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = tx.amount,
            style = type.amountS,
            color = amountColor,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// EMPTY STATES
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EmptyNoTransactionsAtAll(modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Icon container
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
            style = type.titleL.copy(
                fontSize = 15.sp,
                letterSpacing = (-0.075).sp,
            ),
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
private fun EmptyFilteredNoResults(
    query: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Headline — query in accent mono
        if (query.isNotEmpty()) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontFamily = type.labelM.fontFamily,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.W600,
                            letterSpacing = (-0.07).sp,
                        )
                    ) { append("Sin resultados para ") }
                    withStyle(
                        SpanStyle(
                            color = colors.accent,
                            fontFamily = PlexMonoFontFamily,
                            fontSize = 14.sp,
                        )
                    ) { append("«$query»") }
                },
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = "Sin movimientos con esos filtros",
                style = type.labelM.copy(fontSize = 14.sp, letterSpacing = (-0.07).sp),
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Probá con otro nombre, otro monto, o limpiá los filtros activos.",
            style = type.caption.copy(lineHeight = 18.sp),
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp),
        )
        Spacer(Modifier.height(20.dp))
        // "Limpiar filtros" pill button
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

// ═══════════════════════════════════════════════════════════════
// PREVIEWS
// ═══════════════════════════════════════════════════════════════

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
