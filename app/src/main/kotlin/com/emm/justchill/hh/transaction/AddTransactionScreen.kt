package com.emm.justchill.hh.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.account.Account
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.Numpad
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.CtaTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.allColors
import com.emm.justchill.hh.transaction.sheets.AccountPickerSheet
import com.emm.justchill.hh.transaction.sheets.CategoryPickerSheet
import com.emm.justchill.hh.transaction.sheets.DatePickerSheet

@Composable
fun AddTransactionScreen(
    vm: AddTransactionViewModel,
    popBackStack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onAddNewCategory: () -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                AddTransactionEffect.TransactionSaved -> popBackStack()
                is AddTransactionEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AddTransactionScreenContent(
        state = state,
        onIntent = vm::onIntent,
        popBackStack = popBackStack,
        onAddNewCategory = onAddNewCategory,
    )
}

@Composable
private fun AddTransactionScreenContent(
    state: AddTransactionUiState,
    onIntent: (AddTransactionIntent) -> Unit,
    popBackStack: () -> Unit,
    onAddNewCategory: () -> Unit = {},
) {
    val colors = LocalEmmColors.current

    // Sheet visibility state
    var showAccountSheet by rememberSaveable { mutableStateOf(false) }
    var showCategorySheet by rememberSaveable { mutableStateOf(false) }
    var showDateSheet by rememberSaveable { mutableStateOf(false) }

    // Note toggle + field
    var noteExpanded by rememberSaveable { mutableStateOf(state.description.isNotEmpty()) }
    val noteFocusRequester = remember { FocusRequester() }

    val isSpend = state.transactionType == TransactionType.Spend

    // Formatted CTA sublabel
    val ctaAmount = remember(state.amount) {
        val value = centsToSoles(state.amount)
        "S/ ${formatCentsForDisplay(state.amount)}"
    }
    val ctaLabel = if (isSpend) "Anotar gasto" else "Anotar ingreso"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // ─── Top bar ─────────────────────────────────────────────
        JcTopBar(
            title = if (isSpend) "Nuevo gasto" else "Nuevo ingreso",
            left = {
                IconBtn(
                    icon = Icons.Outlined.Close,
                    onClick = popBackStack,
                )
            },
            right = null,
        )

        // ─── Sign toggle ──────────────────────────────────────────
        SignToggle(
            isSpend = isSpend,
            onIncomeClick = { onIntent(AddTransactionIntent.OnTransactionTypeChange(TransactionType.Income)) },
            onSpendClick = { onIntent(AddTransactionIntent.OnTransactionTypeChange(TransactionType.Spend)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 2.dp),
        )

        // ─── Amount hero ──────────────────────────────────────────
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
            )
        }

        // ─── Quick chips row ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Cuenta chip
            QuickChip(
                eyebrow = "CUENTA",
                value = state.accountSelected?.name ?: "—",
                dotColor = state.accountSelected?.let {
                    accountChipDotColor(it.name, colors)
                },
                onClick = { showAccountSheet = true },
                modifier = Modifier.weight(1f),
            )

            // Categoría chip
            QuickChip(
                eyebrow = "CATEGORÍA",
                value = state.categorySelected?.name ?: "—",
                dotColor = state.categorySelected?.color?.primary,
                onClick = { showCategorySheet = true },
                modifier = Modifier.weight(1f),
            )

            // Fecha chip
            QuickChip(
                eyebrow = "FECHA",
                value = state.date,
                dotColor = null,
                onClick = { showDateSheet = true },
                modifier = Modifier.weight(1f),
            )
        }

        // ─── Note toggle ──────────────────────────────────────────
        if (!noteExpanded) {
            NoteToggleButton(
                onClick = { noteExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            )
        } else {
            NoteField(
                value = state.description,
                onValueChange = { onIntent(AddTransactionIntent.OnDescriptionChange(it)) },
                focusRequester = noteFocusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            )
            LaunchedEffect(Unit) {
                noteFocusRequester.requestFocus()
            }
        }

        // ─── Flex spacer ──────────────────────────────────────────
        Spacer(Modifier.weight(1f))

        // ─── Custom numpad ────────────────────────────────────────
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

        // ─── CTA ──────────────────────────────────────────────────
        StickyCTA(
            label = ctaLabel,
            sublabel = ctaAmount,
            inlineSublabel = true,
            tone = CtaTone.Accent,
            enabled = state.isEnabled,
            onClick = { onIntent(AddTransactionIntent.OnSave) },
        )
    }

    // ─── Bottom sheets ────────────────────────────────────────────
    if (showAccountSheet) {
        AccountPickerSheet(
            accounts = state.accounts,
            selectedAccountId = state.accountSelected?.accountId?.value,
            onSelected = { onIntent(AddTransactionIntent.OnAccountSelected(it)) },
            onDismiss = { showAccountSheet = false },
        )
    }

    if (showCategorySheet) {
        CategoryPickerSheet(
            categories = state.categories,
            selectedCategoryId = state.categorySelected?.categoryId?.value,
            onSelected = { onIntent(AddTransactionIntent.OnCategorySelected(it)) },
            onAddNew = { onAddNewCategory() },
            onDismiss = { showCategorySheet = false },
        )
    }

    if (showDateSheet) {
        DatePickerSheet(
            currentMillis = DateUtils.currentDateInMillis(),
            onConfirm = { millis ->
                onIntent(AddTransactionIntent.OnDateChangeInMillis(millis))
            },
            onDismiss = { showDateSheet = false },
        )
    }
}

// ─── Sign toggle ─────────────────────────────────────────────────────────────

@Composable
private fun SignToggle(
    isSpend: Boolean,
    onIncomeClick: () -> Unit,
    onSpendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val containerShape = RoundedCornerShape(12.dp)
    val cellShape = RoundedCornerShape(9.dp)

    Row(
        modifier = modifier
            .clip(containerShape)
            .background(colors.surface1)
            .border(1.dp, colors.border, containerShape)
            .padding(3.dp)
            .height(38.dp),
    ) {
        // Income cell
        val incomeActive = !isSpend
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(cellShape)
                .then(
                    if (incomeActive)
                        Modifier
                            .background(colors.surface3)
                            .border(1.dp, colors.borderFocus, cellShape)
                    else Modifier
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onIncomeClick,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "+",
                color = colors.success,
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                fontFamily = com.emm.justchill.core.theme.PlexMonoFontFamily,
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = "Ingreso",
                fontSize = 12.sp,
                fontWeight = if (incomeActive) FontWeight.W600 else FontWeight.W500,
                fontFamily = InterFontFamily,
                color = if (incomeActive) colors.textPrimary else colors.textTertiary,
            )
        }

        // Spend cell
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(cellShape)
                .then(
                    if (isSpend)
                        Modifier
                            .background(colors.surface3)
                            .border(1.dp, colors.borderFocus, cellShape)
                    else Modifier
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSpendClick,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "−",
                color = colors.danger,
                fontSize = 14.sp,
                fontWeight = FontWeight.W700,
                fontFamily = com.emm.justchill.core.theme.PlexMonoFontFamily,
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = "Gasto",
                fontSize = 12.sp,
                fontWeight = if (isSpend) FontWeight.W600 else FontWeight.W500,
                fontFamily = InterFontFamily,
                color = if (isSpend) colors.textPrimary else colors.textTertiary,
            )
        }
    }
}

// ─── Quick chip ───────────────────────────────────────────────────────────────

@Composable
private fun QuickChip(
    eyebrow: String,
    value: String,
    dotColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val chipShape = RoundedCornerShape(10.dp)

    Row(
        modifier = modifier
            .clip(chipShape)
            .background(colors.surface1)
            .border(1.dp, colors.border, chipShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Eyebrow(text = eyebrow)
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = colors.textPrimary,
                letterSpacing = (-0.06).sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.textDisabled,
            modifier = Modifier.size(11.dp),
        )
    }
}

// ─── Note toggle + field ──────────────────────────────────────────────────────

@Composable
private fun NoteToggleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(11.dp),
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = "Agregar nota",
            fontSize = 12.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
            color = colors.textTertiary,
        )
    }
}

@Composable
private fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current

    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(
                text = "Una nota",
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                color = colors.textTertiary,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= 80) onValueChange(it) },
            singleLine = true,
            cursorBrush = SolidColor(colors.accentFocus),
            textStyle = TextStyle(
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                color = colors.textPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

/**
 * Maps a well-known account name to a dot color for the chip.
 * Mirrors [accountSwatchColor] but is internal to the screen.
 */
private fun accountChipDotColor(
    name: String,
    colors: com.emm.justchill.core.theme.EmmColors,
): Color {
    val lower = name.lowercase()
    return when {
        "yape" in lower -> colors.catMauve
        "plin" in lower -> colors.catSage
        "bcp" in lower -> colors.catSlate
        "bbva" in lower -> colors.catTerracotta
        "interbank" in lower -> colors.catOchre
        "scotiabank" in lower -> colors.catMauve
        "efectivo" in lower -> colors.catOchre
        else -> colors.catGraphite
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF191919, heightDp = 900)
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
                            icon = AppIconCatalog.catalog[it],
                            color = allColors[it],
                            categoryType = CategoryType.Income,
                        ),
                    )
                }
            }
        }
        AddTransactionScreenContent(
            state = AddTransactionUiState(
                categories = categories,
                amount = "8540",
                transactionType = TransactionType.Spend,
            ),
            onIntent = {},
            popBackStack = {},
        )
    }
}
