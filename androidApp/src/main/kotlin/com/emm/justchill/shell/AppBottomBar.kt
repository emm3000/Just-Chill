package com.emm.justchill.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.navigation.BottomBarRoute
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import com.emm.justchill.feature.account.AccountsRoute
import com.emm.justchill.feature.profile.ProfileRoute
import com.emm.justchill.feature.report.ReportRoute
import com.emm.justchill.feature.transaction.SeeTransactionRoute

// "Movimientos" at font scale 1.3 fits a 360dp-wide bar's slot only from three quarters of caption down.
private const val LABEL_MIN_SCALE: Float = 0.75f

private class BarTab(val route: BottomBarRoute, val label: String, val icon: ImageVector)

private val LEADING_TABS: List<BarTab> = listOf(
    BarTab(SeeTransactionRoute, "Movimientos", Icons.AutoMirrored.Outlined.ReceiptLong),
    BarTab(ReportRoute, "Reporte", Icons.Outlined.Insights),
)

private val TRAILING_TABS: List<BarTab> = listOf(
    BarTab(AccountsRoute, "Cuentas", Icons.Outlined.AccountBalanceWallet),
    BarTab(ProfileRoute, "Más", Icons.Outlined.MoreHoriz),
)

@Composable
fun AppBottomBar(
    current: BottomBarRoute,
    profileNeedsAttention: Boolean,
    onSelectTab: (BottomBarRoute) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors: EmmColors = LocalEmmColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Hairline()
        Row(verticalAlignment = Alignment.CenterVertically) {
            LEADING_TABS.forEach { tab ->
                TabSlot(
                    tab = tab,
                    selected = tab.route == current,
                    needsAttention = false,
                    onClick = { onSelectTab(tab.route) },
                    modifier = Modifier.weight(1f),
                )
            }
            AddKey(onClick = onAdd, modifier = Modifier.weight(1f))
            TRAILING_TABS.forEach { tab ->
                TabSlot(
                    tab = tab,
                    selected = tab.route == current,
                    needsAttention = tab.route == ProfileRoute && profileNeedsAttention,
                    onClick = { onSelectTab(tab.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabSlot(
    tab: BarTab,
    selected: Boolean,
    needsAttention: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val type: EmmType = LocalEmmType.current
    val tint: Color = if (selected) colors.textPrimary else colors.textTertiary
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val accessibleName: String = if (needsAttention) "${tab.label}, requiere tu atención" else tab.label

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = spacing.s16)
            .semantics { contentDescription = accessibleName }
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = spacing.s2),
    ) {
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(radii.rFull)
                    .indication(interactionSource, ripple(color = colors.textPrimary))
                    .padding(horizontal = spacing.s4, vertical = spacing.s1),
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(spacing.s6),
                )
            }
            if (needsAttention) {
                AttentionDot(modifier = Modifier.align(Alignment.TopEnd).padding(end = spacing.s3))
            }
        }
        Text(
            text = tab.label,
            style = type.caption,
            color = tint,
            autoSize = TextAutoSize.StepBased(
                minFontSize = type.caption.fontSize * LABEL_MIN_SCALE,
                maxFontSize = type.caption.fontSize,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = spacing.s1)
                .clearAndSetSemantics {},
        )
    }
}

@Composable
private fun AttentionDot(modifier: Modifier = Modifier) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Box(
        modifier = modifier
            .size(spacing.s2)
            .clip(CircleShape)
            .background(colors.bg)
            .padding(spacing.hairline)
            .clip(CircleShape)
            .background(colors.warning),
    )
}

@Composable
private fun AddKey(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.heightIn(min = spacing.s16),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(spacing.s12)
                .clip(radii.rM)
                .background(colors.textPrimary)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = colors.bg),
                    role = Role.Button,
                    onClick = onClick,
                ),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Anotar movimiento",
                tint = colors.bg,
                modifier = Modifier.size(spacing.s6),
            )
        }
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun AppBottomBarPreview() {
    EmmTheme {
        AppBottomBar(current = SeeTransactionRoute, profileNeedsAttention = false, onSelectTab = {}, onAdd = {})
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun AppBottomBarAttentionPreview() {
    EmmTheme {
        AppBottomBar(current = SeeTransactionRoute, profileNeedsAttention = true, onSelectTab = {}, onAdd = {})
    }
}
