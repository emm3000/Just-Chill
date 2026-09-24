package com.emm.justchill.feature.transaction.capture.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.FrequentComboChip
import com.emm.justchill.core.ui.category.resolvedColor
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.feature.transaction.capture.FrequentComboUi

@Composable
internal fun FrequentCombos(
    combos: List<FrequentComboUi>,
    selectedAccountId: String?,
    selectedCategoryId: String?,
    onSelect: (FrequentComboUi) -> Unit,
    wrapsUnderLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.s1)) {
        if (wrapsUnderLabel) {
            Eyebrow(text = "Tus combinaciones frecuentes", modifier = Modifier.padding(start = spacing.s6))
            FlowRow(
                modifier = Modifier.padding(horizontal = spacing.s4),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                verticalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                ComboChips(combos, selectedAccountId, selectedCategoryId, onSelect)
            }
        } else {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = spacing.s4),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                ComboChips(combos, selectedAccountId, selectedCategoryId, onSelect)
            }
        }
    }
}

@Composable
private fun ComboChips(
    combos: List<FrequentComboUi>,
    selectedAccountId: String?,
    selectedCategoryId: String?,
    onSelect: (FrequentComboUi) -> Unit,
) {
    val colors: EmmColors = LocalEmmColors.current

    combos.forEach { combo ->
        FrequentComboChip(
            label = combo.label,
            dotColor = combo.colorId?.let(colors::resolvedColor),
            onClick = { onSelect(combo) },
            active = combo.accountId == selectedAccountId && combo.categoryId == selectedCategoryId,
        )
    }
}
