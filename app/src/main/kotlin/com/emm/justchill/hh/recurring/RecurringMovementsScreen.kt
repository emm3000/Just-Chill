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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Repeat
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.Hairline

@Composable
fun RecurringMovementsScreen(
    state: RecurringMovementsUiState,
    onIntent: (RecurringMovementsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current

    Column(
        modifier = modifier
            .background(colors.bg)
            .statusBarsPadding(),
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

        if (state.items.isEmpty()) {
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
                items(state.items, key = { it.id }) { item ->
                    RecurringMovementRow(
                        item = item,
                        onEdit = { onIntent(RecurringMovementsIntent.NavigateToEdit(item.id)) },
                        onDelete = { onIntent(RecurringMovementsIntent.RequestDelete(item.id)) },
                    )
                }
            }
        }
    }

    state.pendingDelete?.let { id ->
        val item = state.items.find { it.id == id }
        DeleteRecurringDialog(
            name = item?.name ?: "",
            onConfirm = { onIntent(RecurringMovementsIntent.ConfirmDelete) },
            onDismiss = { onIntent(RecurringMovementsIntent.DismissDelete) },
        )
    }
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
private fun RecurringMovementRow(item: RecurringMovementUi, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalEmmColors.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val tintColor = if (item.type == TransactionType.Income) colors.success else colors.danger
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tintColor.copy(alpha = 0.18f))
                    .border(1.dp, tintColor.copy(alpha = 0.32f), RoundedCornerShape(8.dp)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Repeat,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(16.dp),
                )
            }

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
                val typeLabel = if (item.type == TransactionType.Income) "Ingreso" else "Gasto"
                val activeLabel = if (item.isActive) "Activo" else "Inactivo"
                Text(
                    text = "$typeLabel · Día ${item.dayOfMonth} · $activeLabel · ${item.formattedAmount}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W400,
                    fontFamily = InterFontFamily,
                    color = colors.textTertiary,
                )
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
        text = { Text("El movimiento recurrente se borrará permanentemente. Los movimientos ya confirmados no se afectan.") },
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
private fun RecurringEmptyState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Repeat,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Sin movimientos recurrentes",
            fontSize = 18.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Crea plantillas para tus pagos mensuales fijos",
            fontSize = 13.sp,
            fontFamily = InterFontFamily,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(20.dp))
        AddRecurringButton(onClick = onAdd)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF191919, heightDp = 800)
@Composable
private fun RecurringMovementsScreenPreview() {
    EmmTheme {
        RecurringMovementsScreen(
            state = RecurringMovementsUiState(
                items = listOf(
                    RecurringMovementUi(
                        id = "1",
                        name = "Netflix",
                        type = TransactionType.Spend,
                        formattedAmount = "- S/ 18.00",
                        isVariableAmount = false,
                        dayOfMonth = 15,
                        isActive = true,
                    ),
                    RecurringMovementUi(
                        id = "2",
                        name = "Sueldo BCP",
                        type = TransactionType.Income,
                        formattedAmount = "+ S/ 3,500.00",
                        isVariableAmount = false,
                        dayOfMonth = 1,
                        isActive = true,
                    ),
                ),
            ),
            onIntent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
