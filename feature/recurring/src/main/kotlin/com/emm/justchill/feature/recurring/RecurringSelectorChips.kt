package com.emm.justchill.feature.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.ui.atoms.FormSection
import com.emm.justchill.core.ui.atoms.SelectorChip
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.category.resolvedColor
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.transaction.Catalog

@Composable
internal fun SelectorChipsRow(
    state: AddEditRecurringMovementUiState,
    onOpenAccount: () -> Unit,
    onOpenCategory: () -> Unit,
    onOpenDay: () -> Unit,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val categoryDotColor: Color = state.selectedCategory?.resolvedColor?.primary ?: colors.catGraphite

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        FormSection(eyebrow = "CUENTA", modifier = Modifier.weight(1f)) {
            SelectorChip(
                label = state.selectedAccount?.name ?: "Seleccionar",
                dotColor = null,
                onClickLabel = "Cambiar la cuenta",
                onClick = onOpenAccount,
            )
        }
        FormSection(eyebrow = "CATEGORÍA", modifier = Modifier.weight(1f)) {
            SelectorChip(
                label = state.selectedCategory?.name ?: "Sin categoría",
                dotColor = categoryDotColor,
                onClickLabel = "Cambiar la categoría",
                onClick = onOpenCategory,
            )
        }
        FormSection(eyebrow = "DÍA", modifier = Modifier.weight(1f)) {
            SelectorChip(
                label = "Día ${state.dayOfMonth}",
                dotColor = null,
                onClickLabel = "Cambiar el día del mes",
                onClick = onOpenDay,
                trailingIcon = Icons.Outlined.CalendarMonth,
            )
        }
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun SelectorChipsRowOverflowPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Box(
            modifier = Modifier
                .background(colors.bg)
                .padding(spacing.s4),
        ) {
            SelectorChipsRow(
                state = AddEditRecurringMovementUiState(
                    catalog = Catalog.Loaded(
                        accounts = listOf(
                            Account(
                                accountId = AccountId("1"),
                                name = "Tarjeta de crédito BCP",
                            ),
                        ),
                        categories = mapOf(
                            CategoryType.Spend to listOf(
                                SelectableCategory(
                                    categoryId = CategoryId("1"),
                                    name = "Cuidado personal y salud",
                                    iconId = "wallet",
                                    categoryType = CategoryType.Spend,
                                    colorId = "green",
                                ),
                            ),
                        ),
                    ),
                    accountId = AccountId("1"),
                    categoryId = CategoryId("1"),
                    dayOfMonth = 31,
                ),
                onOpenAccount = {},
                onOpenCategory = {},
                onOpenDay = {},
            )
        }
    }
}
