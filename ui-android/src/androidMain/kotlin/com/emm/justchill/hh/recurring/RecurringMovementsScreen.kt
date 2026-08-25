package com.emm.justchill.hh.recurring

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.shared.Money
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.components.EmmCard
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.Pill
import com.emm.justchill.core.ui.atoms.PillTone
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.shared.formatNeutral
import com.emm.justchill.hh.shared.fromCentsToSolesWith

@Composable
fun RecurringMovementsScreen(
    state: RecurringMovementsUiState,
    onIntent: (RecurringMovementsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val hasItems = state.activeItems.isNotEmpty() || state.pausedItems.isNotEmpty()

    Column(
        modifier = modifier
            .background(colors.bg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recurrentes",
                fontSize = 28.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterFontFamily,
                color = colors.textPrimary,
                letterSpacing = (-0.4).sp,
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
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
            ) {
                item {
                    RecurringSummaryCard(
                        entranFormatted = state.entranFormatted,
                        salenFormatted = state.salenFormatted,
                        variableCount = state.variableCount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
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
        val item = remember(id, state.activeItems, state.pausedItems) {
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
    val colors = LocalEmmColors.current

    EmmCard(modifier = modifier) {
        Column {
            Eyebrow(text = "Cada mes se repiten")

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Eyebrow(text = "Entran", color = colors.textDisabled)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = entranFormatted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W600,
                        fontFamily = InterFontFamily,
                        color = colors.success,
                        letterSpacing = (-0.15).sp,
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(colors.border),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Eyebrow(text = "Salen", color = colors.textDisabled)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = salenFormatted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W600,
                        fontFamily = InterFontFamily,
                        color = colors.textPrimary,
                        letterSpacing = (-0.15).sp,
                    )
                }
            }

            if (variableCount >= 1) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "$variableCount recurrente(s) de monto variable — no se suma hasta confirmarlo",
                    fontSize = 11.sp,
                    fontFamily = InterFontFamily,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun DayBadge(dayOfMonth: Int) {
    val colors = LocalEmmColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface2)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
    ) {
        Text(
            text = dayOfMonth.toString().padStart(2, '0'),
            fontSize = 12.sp,
            fontWeight = FontWeight.W700,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
            letterSpacing = (-0.1).sp,
        )
        Text(
            text = "DEL MES",
            fontSize = 9.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
            color = colors.textTertiary,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
private fun RecurringMovementRow(
    item: RecurringMovementUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    trailingBadge: (@Composable () -> Unit)? = null,
) {
    val colors = LocalEmmColors.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DayBadge(item.dayOfMonth)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    fontFamily = InterFontFamily,
                    color = colors.textPrimary,
                    letterSpacing = (-0.15).sp,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.categoryColor?.let { colorKey ->
                        val dotColor = remember(colorKey) { findById(colorKey).primary }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${item.categoryName} · ${item.accountName}",
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            color = colors.textTertiary,
                        )
                    } ?: run {
                        Text(
                            text = item.accountName,
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            color = colors.textTertiary,
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (item.isVariableAmount) {
                    Text(
                        text = "Variable",
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        fontStyle = FontStyle.Italic,
                        color = colors.textTertiary,
                    )
                } else {
                    val amountColor = if (item.type == TransactionType.Income) colors.success else colors.textPrimary
                    Text(
                        text = item.formattedAmount,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W600,
                        fontFamily = InterFontFamily,
                        color = amountColor,
                        letterSpacing = (-0.1).sp,
                    )
                }
                trailingBadge?.let {
                    Spacer(Modifier.height(3.dp))
                    it()
                }
            }

            RecurringRowMenu(onEdit = onEdit, onDelete = onDelete)
        }
        Hairline()
    }
}

@Composable
private fun RecurringRowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalEmmColors.current
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded },
                ),
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Opciones",
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.surface2,
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Editar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = InterFontFamily,
                        color = colors.textPrimary,
                        letterSpacing = (-0.15).sp,
                    )
                },
                leadingIcon = { MenuIcon(Icons.Outlined.Edit) },
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Borrar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = InterFontFamily,
                        color = colors.danger,
                        letterSpacing = (-0.15).sp,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun MenuIcon(icon: ImageVector) {
    val colors = LocalEmmColors.current
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = colors.textSecondary,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
private fun DeleteRecurringDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalEmmColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Borrar «$name»?") },
        text = {
            Text("El movimiento recurrente se borrará permanentemente. Los movimientos ya confirmados no se afectan.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Borrar", color = colors.danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun AddRecurringButton(onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = Modifier
            .clip(shape)
            .background(colors.surface3)
            .border(1.dp, colors.borderFocus, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Nueva",
            fontSize = 13.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun RecurringEmptyState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Aún no tienes recurrentes",
            fontSize = 18.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Define un pago o cobro que se repite cada mes y confírmalo" +
                " con un toque cuando toque, en vez de re-escribirlo.",
            fontSize = 13.sp,
            fontFamily = InterFontFamily,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(20.dp))
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
                entranFormatted = formatNeutral(fromCentsToSolesWith(Money(350000L))),
                salenFormatted = formatNeutral(fromCentsToSolesWith(Money(1800L))),
                variableCount = 0,
            ),
            onIntent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
