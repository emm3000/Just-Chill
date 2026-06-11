package com.emm.justchill.hh.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmType

/** Pill button shown in the account row trailing slot when the last sync failed. */
@Composable
internal fun RetryPill(onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val radii = LocalEmmRadii.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .wrapContentSize()
            .clip(radii.rFull)
            .border(1.dp, colors.border, radii.rFull)
            .background(colors.surface2)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = "Reintentar",
            style = type.labelM,
            color = colors.textPrimary,
            maxLines = 1,
        )
    }
}
