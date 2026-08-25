package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors

@Composable
fun EmmRowMenu(contentDescription: String, onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded },
                ),
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = contentDescription,
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
