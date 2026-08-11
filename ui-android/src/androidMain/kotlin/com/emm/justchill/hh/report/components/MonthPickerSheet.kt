package com.emm.justchill.hh.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.shared.YearMonth
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.hh.shared.monthAbbrevLabel
import kotlinx.datetime.Month

@Composable
fun MonthPickerSheet(current: YearMonth, onSelect: (YearMonth) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
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
                .padding(start = 24.dp, end = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Selecciona mes",
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = colors.textPrimary,
                letterSpacing = (-0.15).sp,
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.surface1)
                    .border(1.dp, colors.border, CircleShape)
                    .clickable(onClick = onDismiss),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cerrar",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { displayYear-- },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "Año anterior",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = displayYear.toString(),
                style = type.labelL,
                color = colors.textPrimary,
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { displayYear++ },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Año siguiente",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Month.entries) { month ->
                val isActive = month == current.month && displayYear == current.year
                val tileShape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(tileShape)
                        .border(
                            width = 1.dp,
                            color = if (isActive) colors.accent else colors.border,
                            shape = tileShape,
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
