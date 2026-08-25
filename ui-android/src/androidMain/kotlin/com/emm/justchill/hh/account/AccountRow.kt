package com.emm.justchill.hh.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.account.Account
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.EmmRowMenu
import com.emm.justchill.core.ui.atoms.Hairline

@Composable
internal fun AccountRow(account: Account, movementCount: Int, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalEmmColors.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccountIconTile(
                icon = account.type.toIcon(),
                tintColor = accountDotColor(account.name, colors),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    fontFamily = InterFontFamily,
                    color = colors.textPrimary,
                    letterSpacing = (-0.15).sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${account.type.toLabel()} · ${formatMovements(movementCount)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W400,
                    fontFamily = InterFontFamily,
                    color = colors.textTertiary,
                )
            }

            EmmRowMenu(contentDescription = "Opciones de cuenta", onEdit = onEdit, onDelete = onDelete)
        }
        Hairline()
    }
}

private fun formatMovements(count: Int): String = when (count) {
    0 -> "Sin movimientos"
    1 -> "1 movimiento"
    else -> "$count movimientos"
}

@Composable
private fun AccountIconTile(icon: ImageVector, tintColor: Color) {
    val shape = RoundedCornerShape(8.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(shape)
            .background(tintColor.copy(alpha = 0.18f))
            .border(1.dp, tintColor.copy(alpha = 0.32f), shape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(16.dp),
        )
    }
}
