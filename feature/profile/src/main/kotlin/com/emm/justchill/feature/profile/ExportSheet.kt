package com.emm.justchill.feature.profile

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.emm.justchill.core.domain.transaction.TransactionsCsvScope
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

internal class ExportActions(
    val onOpen: () -> Unit,
    val onBackup: () -> Unit,
    val onCsv: (TransactionsCsvScope) -> Unit,
)

@Composable
internal fun ExportSheet(op: ProfileOp, actions: ExportActions, onDismiss: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val idle: Boolean = op == ProfileOp.None

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
    ) {
        Text(
            text = "Exportar",
            style = type.titleM,
            color = colors.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.s6, end = spacing.s6, bottom = spacing.s2),
        )
        ProfileRowWithTrailing(
            icon = Icons.Outlined.FileDownload,
            label = "Respaldo completo",
            meta = "Un archivo para restaurar todo",
            metaIsPrimary = false,
            enabled = idle,
            onClick = actions.onBackup,
            trailing = {},
        )
        ProfileRowWithTrailing(
            icon = Icons.Outlined.CalendarMonth,
            label = "Movimientos de este mes",
            meta = "CSV para Excel",
            metaIsPrimary = false,
            enabled = idle,
            onClick = { actions.onCsv(TransactionsCsvScope.CurrentMonth) },
            trailing = {},
        )
        ProfileRowWithTrailing(
            icon = Icons.Outlined.TableChart,
            label = "Todos los movimientos",
            meta = "CSV con todo tu historial",
            metaIsPrimary = false,
            enabled = idle,
            onClick = { actions.onCsv(TransactionsCsvScope.Everything) },
            trailing = {},
        )
    }
}
