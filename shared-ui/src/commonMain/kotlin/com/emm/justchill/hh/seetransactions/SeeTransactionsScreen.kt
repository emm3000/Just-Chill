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
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Category
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
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
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.shared.SpanishDateFormat
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.transaction.CategoryUi
import com.emm.justchill.hh.transaction.TransactionUi
import com.emm.justchill.hh.transaction.components.TransactionRow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.Uuid
import org.koin.compose.viewmodel.koinViewModel

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        ScreenHeader()

        SearchInput(
            query = state.query,
            onQueryChange = { onIntent(SeeTransactionsIntent.OnQueryChanged(it)) },
        )

        CategoryChipsRow(
            chips = state.topChips,
            overflowCount = state.overflowCount,
            onToggle = { onIntent(SeeTransactionsIntent.OnCategoryToggled(it)) },
            onTodas = { onIntent(SeeTransactionsIntent.OnClearCategoryFilter) },
            onMore = { showFilterSheet = true },
        )

        if (state.activeCategory != null) {
            ActiveFilterBanner(
                categoryName = state.activeCategory.name,
                query = state.query.takeIf { it.isNotBlank() },
                onClear = { onIntent(SeeTransactionsIntent.OnClearCategoryFilter) },
            )
        }

        Hairline()

        when {
            state.hasNoTransactionsAtAll -> EmptyNoTransactionsAtAll(modifier = Modifier.fillMaxSize())

            state.hasNoResultsForFilter -> EmptyFilteredNoResults(
                query = state.query,
                activeCategoryName = state.activeCategory?.name,
                onClear = { onIntent(SeeTransactionsIntent.OnClearFilters) },
                modifier = Modifier.fillMaxSize(),
            )

            else -> DayGroupedList(
                days = state.days,
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

@Composable
private fun SearchInput(query: String, onQueryChange: (String) -> Unit) {
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

@Composable
private fun CategoryChipsRow(
    chips: List<CategoryChipUi>,
    overflowCount: Int,
    onToggle: (String) -> Unit,
    onTodas: () -> Unit,
    onMore: () -> Unit,
) {
    val anySelected = chips.any { it.selected }
    val todasActive = !anySelected

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
    ) {
        item(key = "todas") {
            CategoryChip(
                label = "Todas",
                selected = todasActive,
                onClick = onTodas,
            )
        }

        items(chips, key = { it.id }) { chip ->
            CategoryChip(
                label = chip.name,
                selected = chip.selected,
                onClick = { onToggle(chip.id) },
            )
        }

        if (overflowCount > 0) {
            item(key = "more") {
                MoreChip(count = overflowCount, onClick = onMore)
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    val bgColor = if (selected) colors.textPrimary else colors.bg
    val borderColor = if (selected) colors.textPrimary else colors.border
    val textColor = if (selected) colors.bg else colors.textSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = type.labelM.copy(fontSize = 12.sp, letterSpacing = 0.sp),
            color = textColor,
        )
    }
}

@Composable
private fun MoreChip(count: Int, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .drawBehind {
                val stroke = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f)),
                )
                drawRoundRect(
                    color = colors.borderFocus,
                    style = stroke,
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = "Más ",
            style = type.labelM.copy(fontSize = 12.sp, letterSpacing = 0.sp),
            color = colors.textTertiary,
        )
        Text(
            text = "$count",
            style = type.labelM.copy(
                fontSize = 12.sp,
                letterSpacing = 0.sp,
            ),
            color = colors.textTertiary,
        )
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
private fun DayGroupedList(days: List<DayGroup>, onItemClick: (String) -> Unit) {
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
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val yesterday = today.minus(1, DateTimeUnit.DAY)
            val dayLabel = when (dayGroup.date) {
                today -> "HOY"

                yesterday -> "AYER"

                else ->
                    SpanishDateFormat.fullWeekday(dayGroup.date.dayOfWeek.isoDayNumber).uppercase()
            }
            val dateCaption = "${dayGroup.date.dayOfMonth} ${SpanishDateFormat.fullMonth(dayGroup.date.month)}"

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
                        style = type.eyebrow.copy(fontSize = 11.sp, letterSpacing = 1.0.sp),
                        color = colors.textDisabled,
                    )
                }
            }

            items(dayGroup.transactions, key = TransactionUi::transactionId) { tx ->
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
            text = "Probá con otro nombre, otro monto, o limpiá los filtros activos.",
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
            state = SeeTransactionsUiState(),
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
                    date = 0,
                    readableDate = "HOY",
                    readableTime = "14:30",
                    category = CategoryUi(Icons.Rounded.Category, findById("green")),
                ),
                TransactionUi(
                    transactionId = Uuid.random().toString(),
                    type = TransactionType.Income,
                    amount = formatIncome("3,200.00"),
                    description = "Sueldo",
                    date = 0,
                    readableDate = "HOY",
                    readableTime = "09:00",
                    category = CategoryUi(Icons.Rounded.Category, findById("gray")),
                ),
            )
        }
        val chips = listOf(
            CategoryChipUi(id = "1", name = "Comida", colorId = "green", selected = false),
            CategoryChipUi(id = "2", name = "Transporte", colorId = "blue", selected = false),
            CategoryChipUi(id = "3", name = "Servicios", colorId = "orange", selected = false),
            CategoryChipUi(id = "4", name = "Ocio", colorId = "pink", selected = true),
        )
        SeeTransactionsContent(
            state = SeeTransactionsUiState(
                days = listOf(DayGroup(previewToday(), txs)),
                topChips = chips,
                overflowCount = 15,
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
                days = emptyList(),
                query = "café",
                activeCategory = ActiveCategoryInfo("1", "Comida"),
            ),
            onIntent = {},
            navigateToEdit = {},
        )
    }
}

private fun previewToday(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
