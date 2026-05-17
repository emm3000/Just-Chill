package com.emm.justchill.hh.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.modalScreenInsets
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(FlowPreview::class)
@Composable
fun SelectIconScreen(onBack: () -> Unit = {}) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    var searchQuery: String by remember { mutableStateOf("") }
    val catalogs: SnapshotStateList<IconCatalog> = remember { mutableStateListOf(*AppIconCatalog.catalog.toTypedArray()) }

    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(220L)
            .map { it.normalizeForSearch() }
            .distinctUntilChanged()
            .collectLatest { query ->
                val result = if (query.isBlank()) {
                    AppIconCatalog.catalog
                } else {
                    AppIconCatalog.catalog.filter { catalog ->
                        catalog.cleanKeywords.any { it.contains(query) }
                    }
                }
                catalogs.clear()
                catalogs.addAll(result)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .modalScreenInsets(),
    ) {

        TopBar(title = "Selecciona un icono", onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = spacing.s4)) {
            EmmTextInput(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Buscar iconos...",
                keyboardType = KeyboardType.Text,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(spacing.s4))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(spacing.s2),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                items(catalogs, key = IconCatalog::id) { catalog ->
                    IconTile(catalog)
                }
            }
        }
    }
}

@Composable
private fun IconTile(catalog: IconCatalog) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val radii = LocalEmmRadii.current

    Column(
        modifier = Modifier
            .background(colors.surface1, radii.rS)
            .padding(spacing.s2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = catalog.icon,
                contentDescription = catalog.name,
                tint = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(spacing.s1))
        Text(
            text = catalog.name,
            style = type.caption,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onBack,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Atrás",
                tint = colors.textPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = title,
            style = type.titleL,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun SelectIconScreenPreview() {
    EmmTheme {
        SelectIconScreen()
    }
}
