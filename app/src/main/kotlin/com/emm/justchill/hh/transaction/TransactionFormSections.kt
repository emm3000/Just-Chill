package com.emm.justchill.hh.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.emm.domain.account.Account
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.allColors
import com.emm.justchill.hh.shared.UiStrings
import kotlinx.coroutines.launch

@Composable
internal fun AmountInputSection(
    amount: String,
    transactionType: TransactionType,
    onAmountChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    focusRequester: FocusRequester,
    onNext: () -> Unit,
) {
    AmountHeroInput(
        rawCents = amount,
        onRawCentsChange = onAmountChange,
        type = transactionType,
        focusRequester = focusRequester,
        onNext = onNext,
        onTypeFlip = {
            onTypeChange(
                when (transactionType) {
                    TransactionType.Income -> TransactionType.Spend
                    TransactionType.Spend -> TransactionType.Income
                }
            )
        },
    )
}

@Composable
internal fun CategorySelectorSection(
    categories: List<SelectableCategory>,
    selected: SelectableCategory?,
    onSelect: (SelectableCategory) -> Unit,
    onMore: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val spacing = LocalEmmSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        SectionLabel(text = "CATEGORÍA", modifier = Modifier.padding(contentPadding))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            contentPadding = contentPadding,
        ) {
            items(categories, key = { it.categoryId.value }) { category ->
                CategoryChip(
                    category = category,
                    isSelected = category.categoryId == selected?.categoryId,
                    onClick = { onSelect(category) },
                )
            }
            item { CategoryMoreChip(onClick = onMore) }
        }
    }
}

@Composable
internal fun NoteSection(
    description: String,
    onValueChange: (String) -> Unit,
) {
    var open by rememberSaveable(description.isNotEmpty()) { mutableStateOf(description.isNotEmpty()) }
    if (open) {
        EmmTextInput(
            value = description,
            onValueChange = onValueChange,
            label = "NOTA",
            placeholder = "Opcional",
            singleLine = false,
        )
    } else {
        NoteToggleLink(onClick = { open = true })
    }
}

@Composable
private fun NoteToggleLink(onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = "+ Agregar nota",
        style = type.labelL,
        color = colors.textSecondary,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = spacing.s2),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountPickerBottomSheet(
    show: Boolean,
    accounts: List<Account>,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (show) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
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
                onAccountSelected = onAccountSelected,
                dismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) onDismiss()
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    Text(
        text = text,
        style = type.labelM,
        color = colors.textTertiary,
        modifier = modifier,
    )
}

@Composable
private fun CategoryChip(
    category: SelectableCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current

    val borderColor = if (isSelected) colors.accentFocus else colors.border
    val bgColor = if (isSelected) colors.surface2 else colors.surface1

    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .background(bgColor, radii.rFull)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, radii.rFull)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.s4, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Icon(
            imageVector = category.icon.icon,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = category.name,
            style = type.labelL,
            color = if (isSelected) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun CategoryMoreChip(onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .background(colors.surface1, radii.rFull)
            .border(1.dp, colors.border, radii.rFull)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.s4, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Icon(
            imageVector = Icons.Outlined.MoreHoriz,
            contentDescription = "Más categorías",
            tint = colors.textPrimary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "Más",
            style = type.labelL,
            color = colors.textSecondary,
        )
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
            text = UiStrings.PICK_ACCOUNT,
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

// --- Previews ---

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AmountInputSectionPreview() {
    EmmTheme {
        AmountInputSection(
            amount = "",
            transactionType = TransactionType.Income,
            onAmountChange = {},
            onTypeChange = {},
            focusRequester = remember { FocusRequester() },
            onNext = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CategorySelectorSectionPreview() {
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
        CategorySelectorSection(
            categories = categories,
            selected = categories.first(),
            onSelect = {},
            onMore = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun NoteSectionCollapsedPreview() {
    EmmTheme { NoteSection(description = "", onValueChange = {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun NoteSectionOpenPreview() {
    EmmTheme { NoteSection(description = "Compra del super", onValueChange = {}) }
}
