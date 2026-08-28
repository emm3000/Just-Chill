package com.emm.justchill.hh.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors

private data class BottomTab(
    val route: BottomBarRoute?,
    val label: String,
    val icon: ImageVector,
    val isAdd: Boolean = false,
)

private val BOTTOM_TABS = listOf(
    BottomTab(SeeTransactionRoute, "Movimientos", Icons.AutoMirrored.Outlined.List),
    BottomTab(ReportRoute, "Reporte", Icons.Outlined.Insights),
    BottomTab(null, "Anotar", Icons.Outlined.Add, isAdd = true),
    BottomTab(AccountsRoute, "Cuentas", Icons.Outlined.AccountBalanceWallet),
    BottomTab(ProfileRoute, "Perfil", Icons.Outlined.Person),
)

@Composable
fun HhBottomBar(
    current: BottomBarRoute?,
    onTabClick: (BottomBarRoute) -> Unit,
    onAddClick: () -> Unit,
    showProfileBadge: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg)
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(1.dp)
                .background(colors.border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BOTTOM_TABS.forEach { tab ->
                if (tab.isAdd) {
                    AddBottomBarItem(
                        label = tab.label,
                        onClick = onAddClick,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    RegularBottomBarItem(
                        tab = tab,
                        isActive = tab.route == current,
                        showBadge = showProfileBadge && tab.route == ProfileRoute,
                        onClick = { tab.route?.let(onTabClick) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RegularBottomBarItem(
    tab: BottomTab,
    isActive: Boolean,
    showBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    // textDisabled on bg is 2.0:1, under the 4.5:1 AA floor these 10sp labels need.
    val tint = if (isActive) colors.textPrimary else colors.textSecondary
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = tab.icon,
                contentDescription = if (showBadge) "${tab.label}, requiere tu atención" else tab.label,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
            if (showBadge) {
                // The bg ring keeps the dot legible where it overlaps the icon.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.bg)
                        .padding(1.5.dp)
                        .clip(CircleShape)
                        .background(colors.warning),
                )
            }
        }
        Spacer(Modifier.size(3.dp))
        Text(
            text = tab.label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
            letterSpacing = 0.1.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun AddBottomBarItem(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.accent),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.size(3.dp))
        Text(
            text = label,
            color = colors.accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            letterSpacing = 0.1.sp,
            maxLines = 1,
        )
    }
}
