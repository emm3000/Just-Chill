package com.emm.justchill.hh.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.BuildConfig
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    onCategoriesClick: () -> Unit = {},
    onAccountsClick: () -> Unit = {},
    onRecurringClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onDeleteAccountClick: () -> Unit = {},
    onSyncNowClick: () -> Unit = {},
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        Text(
            text = "Perfil",
            style = type.headlineL,
            color = colors.textPrimary,
            modifier = Modifier.padding(
                start = spacing.s5,
                end = spacing.s5,
                top = spacing.s6,
                bottom = spacing.s4,
            ),
        )

        AccountSection(
            state = state,
            onSignInClick = onSignInClick,
            onSignOutClick = onSignOutClick,
            onDeleteAccountClick = onDeleteAccountClick,
        )

        SectionHeader(text = "Gestionar")
        ProfileGroup {
            ProfileRow(
                icon = Icons.Outlined.Category,
                label = "Categorías",
                meta = "${state.categoryCount} totales",
                metaIsPrimary = true,
                onClick = onCategoriesClick,
            )
            HairlineDivider()
            ProfileRow(
                icon = Icons.Outlined.AccountBalanceWallet,
                label = "Cuentas",
                meta = "${state.accountCount} activas",
                metaIsPrimary = true,
                onClick = onAccountsClick,
            )
            HairlineDivider()
            ProfileRow(
                icon = Icons.Outlined.Repeat,
                label = "Movimientos recurrentes",
                meta = "Pagos y cobros fijos",
                metaIsPrimary = false,
                onClick = onRecurringClick,
            )
        }

        SectionHeader(text = "Respaldo")
        ProfileGroup {
            ProfileRow(
                icon = Icons.Outlined.FileDownload,
                label = "Exportar mi data",
                meta = "Guardar como archivo",
                metaIsPrimary = true,
                onClick = onExportClick,
            )
            HairlineDivider()
            ProfileRow(
                icon = Icons.Outlined.FileUpload,
                label = "Importar respaldo",
                meta = "Reemplaza todo",
                metaIsPrimary = false,
                onClick = onImportClick,
            )
        }

        SectionHeader(text = "App")
        ProfileGroup {
            ProfileRow(
                icon = Icons.Outlined.Info,
                label = "Acerca de JustChill",
                meta = "El manifiesto",
                metaIsPrimary = false,
                onClick = onAboutClick,
            )
            HairlineDivider()
            ProfileRow(
                icon = Icons.Outlined.Shield,
                label = "Privacidad",
                meta = "100% local",
                metaIsPrimary = false,
                onClick = onPrivacyClick,
            )
        }

        if (BuildConfig.DEBUG) {
            SectionHeader(text = "Debug")
            ProfileGroup {
                ProfileRow(
                    icon = Icons.Outlined.Repeat,
                    label = "Sincronizar ahora",
                    meta = if (state.isSyncing) "Sincronizando…" else "Push + Pull manual",
                    metaIsPrimary = state.isSyncing,
                    onClick = onSyncNowClick,
                )
            }
        }

        Spacer(Modifier.height(spacing.s6))
        VersionFooter()
        Spacer(Modifier.height(spacing.s4))
    }
}

@Composable
private fun AccountSection(
    state: ProfileUiState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
) {
    val colors = LocalEmmColors.current
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            onConfirm = {
                showDeleteAccountDialog = false
                onDeleteAccountClick()
            },
            onDismiss = { showDeleteAccountDialog = false },
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(text = "Cuenta")
        ProfileGroup {
            when (val session = state.session) {
                SessionUiState.Initializing,
                SessionUiState.SignedOut,
                -> {
                    ProfileRow(
                        icon = Icons.Outlined.Shield,
                        label = "Iniciar sesión",
                        meta = "Sincroniza tus datos entre dispositivos",
                        metaIsPrimary = false,
                        onClick = onSignInClick,
                    )
                }

                is SessionUiState.SignedIn -> {
                    ProfileRowWithTrailing(
                        icon = Icons.Outlined.AccountCircle,
                        label = session.email ?: "Tu cuenta",
                        meta = syncStatusLabel(state.isSyncing, state.lastSyncedAtMillis),
                        metaIsPrimary = true,
                        onClick = {},
                        trailing = {
                            if (state.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.textTertiary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.textTertiary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        },
                    )
                    HairlineDivider()
                    ProfileRow(
                        icon = Icons.Outlined.Shield,
                        label = "Cerrar sesión",
                        meta = "Tus datos siguen en este teléfono",
                        metaIsPrimary = false,
                        onClick = onSignOutClick,
                    )
                    HairlineDivider()
                    ProfileRow(
                        icon = Icons.Outlined.Delete,
                        label = "Eliminar cuenta",
                        meta = "Borra tu cuenta y tus datos en la nube",
                        metaIsPrimary = false,
                        onClick = { showDeleteAccountDialog = true },
                    )
                }
            }
        }
    }
}

private fun syncStatusLabel(isSyncing: Boolean, lastSyncedAtMillis: Long?): String = when {
    isSyncing -> "Sincronizando…"

    lastSyncedAtMillis != null -> {
        val formatted = SimpleDateFormat("d MMM, HH:mm", Locale("es")).format(Date(lastSyncedAtMillis))
        "Última sincronización: $formatted"
    }

    else -> "Sincronización activa"
}

@Composable
private fun SectionHeader(text: String) {
    val spacing = LocalEmmSpacing.current
    Eyebrow(
        text = text,
        modifier = Modifier.padding(
            start = spacing.s5,
            end = spacing.s5,
            top = spacing.s5,
            bottom = spacing.s2,
        ),
    )
}

@Composable
private fun ProfileGroup(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        content()
    }
}

@Composable
private fun HairlineDivider() {
    val colors = LocalEmmColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.border),
    )
}

@Composable
private fun ProfileRow(icon: ImageVector, label: String, meta: String, metaIsPrimary: Boolean, onClick: () -> Unit) {
    ProfileRowWithTrailing(
        icon = icon,
        label = label,
        meta = meta,
        metaIsPrimary = metaIsPrimary,
        onClick = onClick,
        trailing = {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = LocalEmmColors.current.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        },
    )
}

@Composable
private fun ProfileRowWithTrailing(
    icon: ImageVector,
    label: String,
    meta: String,
    metaIsPrimary: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
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
            .padding(horizontal = spacing.s5, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTileSmall(icon = icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = type.bodyL.copy(fontWeight = FontWeight.W500),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = meta,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.W400,
                color = if (metaIsPrimary) colors.textSecondary else colors.textTertiary,
            )
        }
        trailing()
    }
}

@Composable
private fun IconTileSmall(icon: ImageVector) {
    val colors = LocalEmmColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface2),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun VersionFooter() {
    val colors = LocalEmmColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Versión ${BuildConfig.VERSION_NAME} · alpha",
            fontSize = 12.sp,
            fontFamily = InterFontFamily,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun ProfileScreenPreview() {
    EmmTheme {
        ProfileScreen(
            state = ProfileUiState(categoryCount = 12, accountCount = 5),
        )
    }
}
