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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.account.Account
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.allColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AddTransactionScreen(
    vm: AddTransactionViewModel,
    onOtherCategorySelected: () -> Unit,
    popBackStack: () -> Unit,
    snackbarHostState: SnackbarHostState,
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
        onOtherCategorySelected = onOtherCategorySelected,
        popBackStack = popBackStack,
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddTransactionScreenContent(
    state: AddTransactionUiState,
    onIntent: (AddTransactionIntent) -> Unit,
    onOtherCategorySelected: () -> Unit,
    popBackStack: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val isKeyboardOpen = WindowInsets.isImeVisible
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val (showSelectDate, setShowSelectDate) = remember { mutableStateOf(false) }
    val (showAccountPicker, setShowAccountPicker) = rememberSaveable { mutableStateOf(false) }
    val datePickerState: DatePickerState = rememberDatePickerState()
    val amountFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { amountFocus.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .imePadding(),
    ) {
        ScreenTopBar(
            onClose = {
                keyboard?.hide()
                popBackStack()
            },
            onReset = { onIntent(AddTransactionIntent.OnReset) },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.s4),
            verticalArrangement = Arrangement.spacedBy(spacing.s6),
        ) {

            Spacer(Modifier.height(spacing.s2))

            TypeToggle(
                selected = state.transactionType,
                onSelect = { onIntent(AddTransactionIntent.OnTransactionTypeChange(it)) },
            )

            AmountHeroInput(
                value = state.amount,
                onValueChange = { onIntent(AddTransactionIntent.OnAmountChange(it)) },
                type = state.transactionType,
                focusRequester = amountFocus,
                onNext = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                },
            )

            SectionLabel("CATEGORÍA")
            CategoryGrid(
                selected = state.categorySelected,
                categories = state.categories,
                onSelect = { onIntent(AddTransactionIntent.OnCategorySelected(it)) },
                onMore = onOtherCategorySelected,
            )

            ClickableRow(
                label = "FECHA",
                value = state.date,
                onClick = {
                    focusManager.clearFocus()
                    setShowSelectDate(true)
                },
            )

            ClickableRow(
                label = "CUENTA",
                value = state.accountSelected?.name ?: "Selecciona una cuenta",
                emphasized = state.accountSelected != null,
                onClick = {
                    if (isKeyboardOpen) {
                        scope.launch {
                            focusManager.clearFocus()
                            keyboard?.hide()
                            delay(300L)
                        }.invokeOnCompletion { setShowAccountPicker(true) }
                    } else {
                        setShowAccountPicker(true)
                    }
                },
            )

            EmmTextInput(
                value = state.description,
                onValueChange = { onIntent(AddTransactionIntent.OnDescriptionChange(it)) },
                label = "DESCRIPCIÓN",
                placeholder = "Opcional",
                singleLine = false,
            )

            Spacer(Modifier.height(spacing.s4))
        }

        EmmButton(
            text = "Guardar transacción",
            onClick = { onIntent(AddTransactionIntent.OnSave) },
            enabled = state.isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4)
                .navigationBarsPadding(),
        )
    }

    if (showSelectDate) {
        DatePickerDialog(
            onDismissRequest = { setShowSelectDate(false) },
            confirmButton = {
                EmmButton(
                    text = "Ok",
                    onClick = {
                        onIntent(AddTransactionIntent.OnDateChangeInMillis(datePickerState.selectedDateMillis))
                        setShowSelectDate(false)
                    },
                )
            },
            dismissButton = {
                EmmButton(
                    text = "Cancelar",
                    onClick = { setShowSelectDate(false) },
                    variant = com.emm.justchill.components.EmmButtonVariant.Ghost,
                )
            },
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    BottomSheetDialogForPickAccount(
        setShowAccountPicker = setShowAccountPicker,
        showAccountPicker = showAccountPicker,
        accounts = state.accounts,
        onAccountSelected = { onIntent(AddTransactionIntent.OnAccountSelected(it)) },
    )
}

@Composable
private fun ScreenTopBar(
    onClose: () -> Unit,
    onReset: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val closeInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = closeInteraction,
                    indication = null,
                    onClick = onClose,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Cerrar",
                tint = colors.textPrimary,
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = "Agregar",
            style = type.titleL,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )

        val resetInteraction = remember { MutableInteractionSource() }
        Text(
            text = "Reset",
            style = type.labelL,
            color = colors.textSecondary,
            modifier = Modifier
                .clickable(
                    interactionSource = resetInteraction,
                    indication = null,
                    onClick = onReset,
                )
                .padding(spacing.s2),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    Text(
        text = text,
        style = type.labelM,
        color = colors.textTertiary,
    )
}

@Composable
private fun CategoryGrid(
    selected: SelectableCategory?,
    categories: List<SelectableCategory>,
    onSelect: (SelectableCategory) -> Unit,
    onMore: () -> Unit,
) {
    val spacing = LocalEmmSpacing.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        items(categories, key = { it.categoryId.value }) { category ->
            CategoryTile(
                category = category,
                isSelected = category.categoryId == selected?.categoryId,
                onClick = { onSelect(category) },
            )
        }
        item { CategoryMoreTile(onClick = onMore) }
    }
}

@Composable
private fun CategoryTile(
    category: SelectableCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current

    val borderColor = if (isSelected) colors.accentFocus else colors.border

    Column(
        modifier = Modifier
            .height(90.dp)
            .background(colors.surface1, radii.rS)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, radii.rS)
            .clickable(onClick = onClick)
            .padding(spacing.s2),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = category.icon.icon,
            contentDescription = category.name,
            tint = colors.textPrimary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(spacing.s1))
        Text(
            text = category.name,
            style = type.labelM,
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun CategoryMoreTile(onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = Modifier
            .height(90.dp)
            .background(colors.surface1, radii.rS)
            .border(1.dp, colors.border, radii.rS)
            .clickable(onClick = onClick)
            .padding(spacing.s2),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.MoreHoriz,
            contentDescription = "Más categorías",
            tint = colors.textPrimary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(spacing.s1))
        Text(
            text = "Más",
            style = type.labelM,
            color = colors.textSecondary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDialogForPickAccount(
    setShowAccountPicker: (Boolean) -> Unit,
    showAccountPicker: Boolean,
    accounts: List<Account>,
    onAccountSelected: (Account) -> Unit,
) {
    val colors = LocalEmmColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = { setShowAccountPicker(false) }

    if (showAccountPicker) {
        ModalBottomSheet(
            onDismissRequest = dismiss,
            sheetState = sheetState,
            containerColor = colors.surface1,
            dragHandle = { SheetDragHandle() },
        ) {
            val view = LocalView.current
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = false
                    controller.isAppearanceLightNavigationBars = false
                }
            }
            AccountSelectorContent(
                accounts = accounts,
                onAccountSelected = { onAccountSelected(it) },
                dismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) dismiss()
                    }
                },
            )
        }
    }
}

@Composable
private fun SheetDragHandle() {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val radii = LocalEmmRadii.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.s3, bottom = spacing.s3),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 4.dp)
                .background(colors.textTertiary, radii.rFull),
        )
    }
}

@Composable
private fun AccountSelectorContent(
    accounts: List<Account>,
    onAccountSelected: (Account) -> Unit,
    dismiss: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4),
    ) {
        Text(
            text = "Selecciona una cuenta",
            style = type.titleL,
            color = colors.textPrimary,
            modifier = Modifier.padding(vertical = spacing.s2),
        )

        Spacer(Modifier.height(spacing.s2))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(accounts, key = { it.accountId.value }) { account ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAccountSelected(account)
                            dismiss()
                        }
                        .padding(vertical = spacing.s4)
                        .drawBehind {
                            drawLine(
                                color = colors.border,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1f,
                            )
                        },
                ) {
                    Text(
                        text = account.name,
                        style = type.bodyL,
                        color = colors.textPrimary,
                    )
                }
            }
        }

        Spacer(Modifier.height(spacing.s4))
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 900)
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
                        )
                    )
                }
            }
        }
        AddTransactionScreenContent(
            state = AddTransactionUiState(categories = categories),
            onIntent = {},
            popBackStack = {},
            onOtherCategorySelected = {},
        )
    }
}
