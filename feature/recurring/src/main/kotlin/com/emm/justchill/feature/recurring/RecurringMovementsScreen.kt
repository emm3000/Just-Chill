package com.emm.justchill.feature.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.atoms.EmmDialog
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.IconBtnTone
import com.emm.justchill.core.ui.atoms.Pill
import com.emm.justchill.core.ui.atoms.PillTone
import com.emm.justchill.core.ui.components.EmmCard
import com.emm.justchill.core.ui.format.format
import com.emm.justchill.core.ui.format.formatExpense
import com.emm.justchill.core.ui.format.formatIncome
import com.emm.justchill.core.ui.format.formatNeutral
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun RecurringMovementsScreen(
    state: RecurringMovementsUiState,
    onIntent: (RecurringMovementsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val hasItems: Boolean = state.activeItems.isNotEmpty() || state.pausedItems.isNotEmpty()

    Column(
        modifier = modifier
            .background(colors.bg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.s5, end = spacing.s4, top = spacing.s4, bottom = spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recurrentes",
                style = type.headlineL,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            AddRecurringButton(onClick = { onIntent(RecurringMovementsIntent.NavigateToAdd) })
        }

        Hairline()

        if (!hasItems) {
            RecurringEmptyState(
                onAdd = { onIntent(RecurringMovementsIntent.NavigateToAdd) },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = spacing.s1, bottom = spacing.s3),
            ) {
                item {
                    RecurringSummaryCard(
                        entranFormatted = state.entranFormatted,
                        salenFormatted = state.salenFormatted,
                        variableCount = state.variableCount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.s4, vertical = spacing.s2),
                    )
                }

                items(state.activeItems, key = { it.id }) { item ->
                    RecurringMovementRow(
                        item = item,
                        onEdit = { onIntent(RecurringMovementsIntent.NavigateToEdit(item.id)) },
                        onDelete = { onIntent(RecurringMovementsIntent.RequestDelete(item.id)) },
                    )
                }

                if (state.pausedItems.isNotEmpty()) {
                    item {
                        Eyebrow(
                            text = "Pausados",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = spacing.s4, end = spacing.s4, top = spacing.s3, bottom = spacing.s1),
                        )
                    }
                    items(state.pausedItems, key = { it.id }) { item ->
                        Box(modifier = Modifier.alpha(0.5f)) {
                            RecurringMovementRow(
                                item = item,
                                onEdit = { onIntent(RecurringMovementsIntent.NavigateToEdit(item.id)) },
                                onDelete = { onIntent(RecurringMovementsIntent.RequestDelete(item.id)) },
                                trailingBadge = { Pill(text = "Pausado", tone = PillTone.Neutral) },
                            )
                        }
                    }
                }
            }
        }
    }

    state.pendingDelete?.let { id ->
        val item: RecurringMovementUi? = remember(id, state.activeItems, state.pausedItems) {
            state.activeItems.find { it.id == id } ?: state.pausedItems.find { it.id == id }
        }
        DeleteRecurringDialog(
            name = item?.name.orEmpty(),
            onConfirm = { onIntent(RecurringMovementsIntent.ConfirmDelete) },
            onDismiss = { onIntent(RecurringMovementsIntent.DismissDelete) },
        )
    }
}

@Composable
private fun RecurringSummaryCard(
    entranFormatted: String,
    salenFormatted: String,
    variableCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current

    EmmCard(modifier = modifier) {
        Column {
            Eyebrow(text = "Cada mes se repiten")

            Spacer(Modifier.height(spacing.s2))

            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Eyebrow(text = "Entran", color = colors.textDisabled)
                    Spacer(Modifier.height(spacing.s1))
                    Text(
                        text = entranFormatted,
                        style = type.amountM,
                        color = colors.success,
                    )
                }

                Box(
                    modifier = Modifier
                        .width(spacing.hairline)
                        .height(spacing.s8)
                        .background(colors.border),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = spacing.s3),
                ) {
                    Eyebrow(text = "Salen", color = colors.textDisabled)
                    Spacer(Modifier.height(spacing.s1))
                    Text(
                        text = salenFormatted,
                        style = type.amountM,
                        color = colors.textPrimary,
                    )
                }
            }

            if (variableCount >= 1) {
                Spacer(Modifier.height(spacing.s2))
                Text(
                    text = "$variableCount recurrente(s) de monto variable — no se suma hasta confirmarlo",
                    style = type.caption,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun DeleteRecurringDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current

    EmmDialog(
        title = "¿Borrar «$name»?",
        confirmLabel = "Borrar",
        onConfirm = onConfirm,
        dismissLabel = "Cancelar",
        onDismiss = onDismiss,
        confirmTone = IconBtnTone.Danger,
    ) {
        Text(
            text = "El movimiento recurrente se borrará permanentemente. Los movimientos ya confirmados no se afectan.",
            style = type.bodyM,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun AddRecurringButton(onClick: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val shape: RoundedCornerShape = LocalEmmRadii.current.rFull
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(spacing.s12)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(colors.surface3)
                .border(spacing.hairline, colors.borderFocus, shape)
                .indication(interactionSource, ripple())
                .padding(horizontal = spacing.s3, vertical = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s1),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(spacing.s4),
            )
            Text(
                text = "Nueva",
                style = type.labelL,
                color = colors.textPrimary,
            )
        }
    }
}

@Composable
private fun RecurringEmptyState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current

    Column(
        modifier = modifier.padding(horizontal = spacing.s6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(spacing.s12),
        )
        Spacer(Modifier.height(spacing.s4))
        Text(
            text = "Aún no tienes recurrentes",
            style = type.titleL,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(spacing.s2))
        Text(
            text = "Define un pago o cobro que se repite cada mes y confírmalo" +
                " con un toque cuando toque, en vez de re-escribirlo.",
            style = type.bodyM,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(spacing.s5))
        AddRecurringButton(onClick = onAdd)
    }
}

@Preview
@Composable
private fun RecurringMovementsScreenPreview() {
    EmmTheme {
        RecurringMovementsScreen(
            state = RecurringMovementsUiState(
                activeItems = listOf(
                    RecurringMovementUi(
                        id = "1",
                        name = "Netflix",
                        type = TransactionType.Spend,
                        formattedAmount = formatExpense("18.00"),
                        isVariableAmount = false,
                        dayOfMonth = 15,
                        isActive = true,
                        categoryName = "Entretenimiento",
                        categoryColor = "blue",
                        accountName = "BCP",
                    ),
                    RecurringMovementUi(
                        id = "2",
                        name = "Sueldo BCP",
                        type = TransactionType.Income,
                        formattedAmount = formatIncome("3,500.00"),
                        isVariableAmount = false,
                        dayOfMonth = 1,
                        isActive = true,
                        categoryName = null,
                        categoryColor = null,
                        accountName = "BCP",
                    ),
                ),
                pausedItems = listOf(
                    RecurringMovementUi(
                        id = "3",
                        name = "Agua",
                        type = TransactionType.Spend,
                        formattedAmount = "Variable",
                        isVariableAmount = true,
                        dayOfMonth = 10,
                        isActive = false,
                        accountName = "Efectivo",
                    ),
                ),
                entranFormatted = formatNeutral(Money(350000L).format()),
                salenFormatted = formatNeutral(Money(1800L).format()),
                variableCount = 0,
            ),
            onIntent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
