package com.emm.justchill.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.ui.atoms.MonthChevron
import com.emm.justchill.core.ui.atoms.MonthChevronDirection
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.core.ui.format.monthAbbrevLabel
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import kotlinx.datetime.Month

private const val MONTH_GRID_COLUMNS: Int = 3

@Composable
fun MonthPickerSheet(current: YearMonth, onSelect: (YearMonth) -> Unit, onDismiss: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val radii: EmmRadii = LocalEmmRadii.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var displayYear by rememberSaveable { mutableIntStateOf(current.year) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.s6, end = spacing.s4, bottom = spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Selecciona mes",
                style = type.titleM,
                color = colors.textPrimary,
            )
            val closeInteraction: MutableInteractionSource = remember { MutableInteractionSource() }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(spacing.s12)
                    .clickable(
                        interactionSource = closeInteraction,
                        indication = null,
                        onClick = onDismiss,
                    ),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(spacing.s8)
                        .clip(CircleShape)
                        .background(colors.surface1)
                        .border(spacing.hairline, colors.border, CircleShape)
                        .indication(closeInteraction, ripple()),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(spacing.s3),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s6, vertical = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MonthChevron(
                direction = MonthChevronDirection.Previous,
                onClick = { displayYear-- },
                contentDescription = "Año anterior",
            )
            Text(
                text = displayYear.toString(),
                style = type.labelL,
                color = colors.textPrimary,
            )
            MonthChevron(
                direction = MonthChevronDirection.Next,
                onClick = { displayYear++ },
                contentDescription = "Año siguiente",
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(MONTH_GRID_COLUMNS),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s2)
                .padding(bottom = spacing.s4),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            verticalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            items(Month.entries) { month ->
                val isActive = month == current.month && displayYear == current.year
                Box(
                    modifier = Modifier
                        .height(spacing.s12)
                        .clip(radii.rM)
                        .border(
                            width = spacing.hairline,
                            color = if (isActive) colors.borderFocus else colors.border,
                            shape = radii.rM,
                        )
                        .clickable {
                            onSelect(YearMonth(displayYear, month))
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = YearMonth(displayYear, month).monthAbbrevLabel(),
                        style = type.labelL,
                        color = if (isActive) colors.textPrimary else colors.textSecondary,
                    )
                }
            }
        }
    }
}
