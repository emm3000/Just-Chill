package com.emm.justchill.feature.transaction.capture.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.emm.justchill.core.ui.atoms.SelectorChip
import com.emm.justchill.core.ui.category.resolvedColor
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.feature.transaction.capture.AddTransactionIntent
import com.emm.justchill.feature.transaction.capture.AddTransactionUiState
import com.emm.justchill.feature.transaction.capture.TransactionSheet

@Composable
internal fun PadForm(
    state: AddTransactionUiState,
    onIntent: (AddTransactionIntent) -> Unit,
    onAddNewAccount: () -> Unit,
    arrangement: PadArrangement,
) {
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val isStackedTall: Boolean = arrangement.isStackedTall
    val isNarrow: Boolean = arrangement == PadArrangement.SideBySideNarrow

    Column {
        if (isNarrow) {
            Column(modifier = Modifier.padding(horizontal = spacing.s4)) {
                AccountChip(state, onIntent, onAddNewAccount, Modifier.fillMaxWidth())
                CategoryChip(state, onIntent, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s4),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                AccountChip(state, onIntent, onAddNewAccount, Modifier.weight(ACCOUNT_CHIP_WEIGHT))
                CategoryChip(state, onIntent, Modifier.weight(CATEGORY_CHIP_WEIGHT))
            }
        }

        FormMetaRow(
            dateLabel = state.dateLabel,
            note = state.description,
            onDateClick = { onIntent(AddTransactionIntent.OnSheetRequested(TransactionSheet.Date)) },
            onNoteClick = { onIntent(AddTransactionIntent.OnSheetRequested(TransactionSheet.Note)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (isNarrow) spacing.s4 else spacing.s6,
                    end = if (isNarrow) spacing.s0 else spacing.s6,
                ),
            isCompact = isNarrow,
        )

        if (state.frequentCombos.isNotEmpty()) {
            FrequentCombos(
                combos = state.frequentCombos,
                selectedAccountId = state.accountSelected?.accountId?.value,
                selectedCategoryId = state.categorySelected?.categoryId?.value,
                onSelect = { combo -> onIntent(AddTransactionIntent.OnFrequentComboSelected(combo)) },
                wrapsUnderLabel = isStackedTall,
                modifier = Modifier.padding(vertical = if (isStackedTall) spacing.s3 else spacing.s0),
            )
        }
    }
}

@Composable
private fun AccountChip(
    state: AddTransactionUiState,
    onIntent: (AddTransactionIntent) -> Unit,
    onAddNewAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.hasNoAccounts) {
        SelectorChip(
            label = "Crear cuenta",
            dotColor = null,
            onClickLabel = "Crear una cuenta",
            onClick = onAddNewAccount,
            trailingIcon = Icons.Outlined.Add,
            modifier = modifier,
        )
    } else {
        SelectorChip(
            label = state.accountSelected?.name ?: "—",
            dotColor = null,
            onClickLabel = "Cambiar la cuenta",
            onClick = { onIntent(AddTransactionIntent.OnSheetRequested(TransactionSheet.Account)) },
            modifier = modifier,
        )
    }
}

@Composable
private fun CategoryChip(
    state: AddTransactionUiState,
    onIntent: (AddTransactionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors: EmmColors = LocalEmmColors.current

    SelectorChip(
        label = state.categorySelected?.name ?: "—",
        dotColor = state.categorySelected?.let { colors.resolvedColor(it.colorId) },
        onClickLabel = "Cambiar la categoría",
        onClick = { onIntent(AddTransactionIntent.OnSheetRequested(TransactionSheet.Category)) },
        modifier = modifier,
    )
}
