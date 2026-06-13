package com.emm.justchill.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmButtonVariant
import com.emm.justchill.components.EmmCard
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.hh.shared.formatNeutral

/**
 * Visual reference for the design system components. Open in Android Studio Preview
 * to review tokens together. Not used in production.
 */
@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 1600)
@Composable
private fun EmmComponentsGallery() {
    EmmTheme {
        val colors = LocalEmmColors.current
        val spacing = LocalEmmSpacing.current
        val type = LocalEmmType.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
                .verticalScroll(rememberScrollState())
                .padding(spacing.s4),
            verticalArrangement = Arrangement.spacedBy(spacing.s6),
        ) {
            SectionTitle("Type · amounts")
            Text("+1,234.56", style = type.amountHero, color = colors.textPrimary)
            Text(formatNeutral("4,820.00"), style = type.amountL, color = colors.textPrimary)
            Text("−12.50", style = type.amountM, color = colors.textPrimary)
            Text("−2.40", style = type.amountS, color = colors.textPrimary)

            SectionTitle("Type · text")
            Text("Display heading", style = type.display, color = colors.textPrimary)
            Text("Headline L", style = type.headlineL, color = colors.textPrimary)
            Text("Headline M", style = type.headlineM, color = colors.textPrimary)
            Text("Title L", style = type.titleL, color = colors.textPrimary)
            Text("Body L · default reading size", style = type.bodyL, color = colors.textPrimary)
            Text("Body M · secondary text", style = type.bodyM, color = colors.textSecondary)
            Text("Label L · BUTTON", style = type.labelL, color = colors.textPrimary)
            Text("caption · hace 2 días", style = type.caption, color = colors.textTertiary)

            SectionTitle("Buttons")
            EmmButton("Primary", onClick = {}, modifier = Modifier.fillMaxWidth())
            EmmButton(
                "Secondary",
                onClick = {},
                variant = EmmButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            EmmButton("Ghost", onClick = {}, variant = EmmButtonVariant.Ghost, modifier = Modifier.fillMaxWidth())
            EmmButton(
                "Eliminar",
                onClick = {},
                variant = EmmButtonVariant.Destructive,
                modifier = Modifier.fillMaxWidth(),
            )
            EmmButton("Disabled", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())

            SectionTitle("Text input")
            EmmTextInput(
                value = "",
                onValueChange = {},
                label = "Nombre",
                placeholder = "ejm. Cuenta de ahorros",
            )
            EmmTextInput(
                value = "Comida del lunes",
                onValueChange = {},
                label = "Descripción",
                helper = "Opcional",
            )
            EmmTextInput(
                value = "",
                onValueChange = {},
                label = "Monto",
                placeholder = "0.00",
                isError = true,
                helper = "Ingresa un monto válido",
            )

            SectionTitle("List item · transaction rows")
            Column {
                EmmListItem(
                    icon = Icons.Outlined.ShoppingCart,
                    title = "Mercado",
                    metadata = "Comida · 14:30",
                    amount = "−84.20",
                    categoryColor = colors.catSage,
                    onClick = {},
                )
                EmmListItem(
                    icon = Icons.Outlined.LocalDining,
                    title = "Almuerzo con Sofía",
                    metadata = "Comida · 13:05",
                    amount = "−42.00",
                    categoryColor = colors.catTerracotta,
                    onClick = {},
                )
                EmmListItem(
                    icon = Icons.Outlined.LocalGasStation,
                    title = "Gasolina",
                    metadata = "Transporte · 09:18",
                    amount = "−120.00",
                    categoryColor = colors.catOchre,
                    onClick = {},
                )
                EmmListItem(
                    icon = Icons.Outlined.Bolt,
                    title = "Sueldo",
                    metadata = "Ingreso · 09:00",
                    amount = "+3,200.00",
                    categoryColor = colors.catSlate,
                    onClick = {},
                )
            }

            SectionTitle("Card")
            EmmCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
                    Text("Cuenta principal", style = type.titleM, color = colors.textSecondary)
                    Text(formatNeutral("4,820.00"), style = type.amountL, color = colors.textPrimary)
                    Text("Última actualización · hace 5 min", style = type.caption, color = colors.textTertiary)
                }
            }

            SectionTitle("Category palette")
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                CategorySwatch("slate", colors.catSlate)
                CategorySwatch("sage", colors.catSage)
                CategorySwatch("terra", colors.catTerracotta)
                CategorySwatch("mauve", colors.catMauve)
                CategorySwatch("ochre", colors.catOchre)
                CategorySwatch("graph", colors.catGraphite)
            }

            Spacer(Modifier.height(spacing.s12))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val type = LocalEmmType.current
    val colors = LocalEmmColors.current
    Text(
        text = text,
        style = type.labelM,
        color = colors.textTertiary,
        modifier = Modifier.padding(top = LocalEmmSpacing.current.s4),
    )
}

@Composable
private fun CategorySwatch(name: String, color: Color) {
    val type = LocalEmmType.current
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val radii = LocalEmmRadii.current
    Column(
        modifier = Modifier
            .background(colors.surface1, radii.rS)
            .padding(spacing.s2),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(color, radii.rFull),
        )
        Spacer(Modifier.height(spacing.s1))
        Text(name, style = type.caption, color = colors.textTertiary)
    }
}
