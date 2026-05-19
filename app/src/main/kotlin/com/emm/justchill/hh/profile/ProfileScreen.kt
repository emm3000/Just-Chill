package com.emm.justchill.hh.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.BuildConfig
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onCategoriesClick: () -> Unit = {},
    onAccountsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {

        Text(
            text = "Perfil",
            style = type.headlineL,
            color = colors.textPrimary,
            modifier = Modifier.padding(
                start = spacing.s4,
                end = spacing.s4,
                top = spacing.s6,
                bottom = spacing.s4,
            ),
        )

        Text(
            text = "GESTIONAR",
            style = type.labelM,
            color = colors.textTertiary,
            modifier = Modifier.padding(
                horizontal = spacing.s4,
                vertical = spacing.s2,
            ),
        )

        ProfileRow(
            icon = Icons.Outlined.Category,
            label = "Categorías",
            onClick = onCategoriesClick,
        )
        ProfileRow(
            icon = Icons.Outlined.AccountBalanceWallet,
            label = "Cuentas",
            onClick = onAccountsClick,
        )

        Spacer(Modifier.height(spacing.s2))

        Text(
            text = "RESPALDO",
            style = type.labelM,
            color = colors.textTertiary,
            modifier = Modifier.padding(
                horizontal = spacing.s4,
                vertical = spacing.s2,
            ),
        )

        ProfileRow(
            icon = Icons.Outlined.FileDownload,
            label = "Exportar",
            onClick = onExportClick,
        )
        ProfileRow(
            icon = Icons.Outlined.FileUpload,
            label = "Importar",
            onClick = onImportClick,
        )

        Spacer(Modifier.height(spacing.s2))

        Text(
            text = "APP",
            style = type.labelM,
            color = colors.textTertiary,
            modifier = Modifier.padding(
                horizontal = spacing.s4,
                vertical = spacing.s2,
            ),
        )

        ProfileRow(
            icon = Icons.Outlined.Info,
            label = "Acerca de",
            onClick = onAboutClick,
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = "Versión ${BuildConfig.VERSION_NAME}",
            style = type.caption,
            color = colors.textTertiary,
            modifier = Modifier
                .padding(horizontal = spacing.s4, vertical = spacing.s4)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val labelColor: Color = if (destructive) colors.danger else colors.textPrimary
    val iconColor: Color = if (destructive) colors.danger else colors.textPrimary
    val bg: Color = if (isPressed) colors.surface1 else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = spacing.s4, vertical = spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = type.bodyL,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
        if (!destructive) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun ProfileScreenPreview() {
    EmmTheme {
        ProfileScreen()
    }
}
