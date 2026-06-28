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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
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
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors

// Shared bottom navigation bar for both nav hosts (Android Hh.kt + iOS IosApp.kt). The tab labels are
// Spanish UI copy and are preserved verbatim. `dropUnlessResumed` is baked into the click handlers so
// BOTH platforms get the double-tap nav guard (iOS previously used plain lambdas and gained the guard
// in this slice — a strict improvement that prevents nav races during transitions).

private data class BottomTab(
    val route: BottomBarRoute?, // null = add pseudo-tab
    val label: String,
    val icon: ImageVector,
    val isAdd: Boolean = false,
)

private val BOTTOM_TABS = listOf(
    BottomTab(HomeRoute, "Inicio", Icons.Outlined.Home),
    BottomTab(SeeTransactionRoute, "Ver", Icons.AutoMirrored.Outlined.List),
    BottomTab(null, "Agregar", Icons.Outlined.Add, isAdd = true),
    BottomTab(AccountsRoute, "Cuentas", Icons.Outlined.AccountBalanceWallet),
    BottomTab(ProfileRoute, "Perfil", Icons.Outlined.Person),
)

@Composable
fun HhBottomBar(current: BottomBarRoute?, onTabClick: (BottomBarRoute) -> Unit, onAddClick: () -> Unit) {
    val colors = LocalEmmColors.current

    Column(
        modifier = Modifier
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
                        onClick = dropUnlessResumed(block = onAddClick),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    val isActive = tab.route == current
                    RegularBottomBarItem(
                        label = tab.label,
                        icon = tab.icon,
                        isActive = isActive,
                        onClick = dropUnlessResumed { tab.route?.let(onTabClick) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RegularBottomBarItem(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val tint = if (isActive) colors.textPrimary else colors.textDisabled
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
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(3.dp))
        Text(
            text = label,
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

/**
 * Switches to a bottom-bar tab using the "exit through home" pattern:
 * back stack always starts at [startTab], with the selected tab on top (if different).
 * Each nav host passes its own START_TAB ([SeeTransactionRoute] on Android, [HomeRoute] on iOS).
 */
fun NavBackStack<NavKey>.switchTab(target: BottomBarRoute, startTab: BottomBarRoute) {
    clear()
    add(startTab)
    if (target != startTab) add(target)
}

/**
 * Pop intermediate routes (`CategoryRoute` and any legacy in-between) until
 * the transaction screen is at the top, so it receives `pendingCategory`
 * via its `LaunchedEffect` and the user lands back where they were.
 */
fun NavBackStack<NavKey>.popToTransactionScreen() {
    while (isNotEmpty() && last() !is AddTransactionRoute && last() !is EditTransactionRoute) {
        removeLastOrNull()
    }
}
