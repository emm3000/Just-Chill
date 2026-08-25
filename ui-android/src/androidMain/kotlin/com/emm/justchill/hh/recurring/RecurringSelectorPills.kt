package com.emm.justchill.hh.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.hh.transaction.resolvedColor

@Composable
private fun SelectorPill(
    eyebrow: String,
    value: String,
    onClick: () -> Unit,
    dotColor: Color? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(radii.rM)
            .background(colors.surface1)
            .border(1.dp, colors.border, radii.rM)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Eyebrow(text = eyebrow)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (dotColor != null) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                }
                if (trailingIcon != null) {
                    trailingIcon()
                }
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W500,
                    fontFamily = InterFontFamily,
                    color = colors.textPrimary,
                    letterSpacing = (-0.13).sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun SelectorPillsRow(
    state: AddEditRecurringMovementUiState,
    onOpenAccount: () -> Unit,
    onOpenCategory: () -> Unit,
    onOpenDay: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val categoryDotColor = state.selectedCategory?.resolvedColor?.primary ?: colors.accent

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            SelectorPill(
                eyebrow = "CUENTA",
                value = state.selectedAccount?.name ?: "Seleccionar",
                dotColor = colors.accent,
                onClick = onOpenAccount,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            SelectorPill(
                eyebrow = "CATEGORÍA",
                value = state.selectedCategory?.name ?: "Sin categoría",
                dotColor = categoryDotColor,
                onClick = onOpenCategory,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            SelectorPill(
                eyebrow = "DÍA",
                value = "Día ${state.dayOfMonth}",
                onClick = onOpenDay,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(11.dp),
                    )
                },
            )
        }
    }
}
