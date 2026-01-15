package com.emm.justchill.hh.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.shared.EmmTextInput
import com.emm.justchill.hh.transaction.components.EmmCenteredToolbar
import com.emm.justchill.hh.transaction.components.NewButton
import org.koin.androidx.compose.koinViewModel

private val borderRadius = 10.dp
private val dashWidth = 10f
private val dashGap = 7f
private val strokeWidth = 2.dp

@Composable
fun AddCategoryScreen(
    navController: NavBackStack<NavKey>,
    vm: AddCategoryViewModel = koinViewModel(),
) {

    AddCategoryScreen(
        state = vm.state,
        onAction = vm::onAction,
        navigateToBack = navController::removeLastOrNull
    )
}

@Composable
private fun AddCategoryScreen(
    state: AddCategoryUiState,
    onAction: (AddCategoryAction) -> Unit,
    navigateToBack: () -> Unit = {},
) {

    val current = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            EmmCenteredToolbar(
                title = "Agregar Categoría",
                navigationIconClick = Icons.Default.Close,
                onNavigationIconClick = {
                    current?.hide()
                    navigateToBack()
                },
                actions = {
                    TextButton(
                        onClick = {
                            current?.hide()
                            navigateToBack()
                        }
                    ) {
                        Text(
                            modifier = Modifier,
                            text = "Cancel",
                            fontFamily = LatoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 17.sp
                        )
                    }
                }
            )
        },
        bottomBar = {
            NewButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                enabled = state.isAllFieldValidated,
                onClick = {
                    current?.hide()
                    onAction(AddCategoryAction.OnSave)
                    navigateToBack()
                },
                title = "Guardar categoría",
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {

            PreviewSection(state)

            Spacer(modifier = Modifier.height(20.dp))

            EmmTextInput(
                modifier = Modifier,
                label = "Nombre",
                placeholder = "Ingresa el nombre",
                value = state.name,
                onChange = { onAction(AddCategoryAction.OnNameChange(it)) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            CategoryTypeToggle(
                modifier = Modifier,
                selectedType = state.categoryType,
                onTypeSelected = { onAction(AddCategoryAction.OnCategoryTypeChange(it)) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            IconPicker(state.icon) {
                onAction(AddCategoryAction.OnIconChange(it))
            }

            Spacer(modifier = Modifier.height(20.dp))

            ColorPicker(state.color) {
                onAction(AddCategoryAction.OnColorChange(it))
            }
        }
    }
}

@Composable
fun ColumnScope.ColorPicker(
    selectedColor: CategoryColor,
    onColorChange: (CategoryColor) -> Unit = {}
) {
    Text(
        modifier = Modifier.align(Alignment.Start),
        text = "Color",
        fontFamily = LatoFontFamily
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        maxItemsInEachRow = 10,
    ) {
        allColors.forEach { color ->
            val isSelected = color == selectedColor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = CircleShape
                        ) else Modifier
                    )
                    .background(color.primary)
                    .selectable(
                        selected = true,
                        onClick = {
                            onColorChange(color)
                        }
                    ),
            )
        }
    }

}

@Composable
fun ColumnScope.IconPicker(
    selectedIcon: IconCatalog,
    onIconChange: (IconCatalog) -> Unit = {}
) {
    Text(
        modifier = Modifier.align(Alignment.Start),
        text = "Icono",
        fontFamily = LatoFontFamily
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
        Text(
            text = "Buscar iconos . . .",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            fontFamily = LatoFontFamily
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        maxItemsInEachRow = 6,
    ) {
        AppIconCatalog.catalog.take(11).forEach {
            val isSelected = it == selectedIcon
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (isSelected) Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(10.dp)
                        ) else Modifier
                    )
                    .background(MaterialTheme.colorScheme.surface)
                    .selectable(
                        selected = isSelected,
                        onClick = {
                            onIconChange(it)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = it.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

}

@Composable
private fun PreviewSection(state: AddCategoryUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(state.color.darkContainer)
            .drawBehind {
                val stroke = Stroke(
                    width = strokeWidth.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(dashWidth, dashGap),
                        phase = 0f
                    )
                )
                drawRoundRect(
                    color = state.color.onPrimary,
                    style = stroke,
                    cornerRadius = CornerRadius(borderRadius.toPx())
                )
            }
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(state.color.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = state.icon.icon,
                    contentDescription = null,
                    tint = state.color.onPrimary,
                )
            }
            Text(
                text = state.name,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Bold,
                color = state.color.onPrimary,
            )
            Text(
                text = state.categoryType.label,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Bold,
                color = state.color.onPrimary.copy(alpha = 0.5f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoryPreview() {

    EmmTheme {
        AddCategoryScreen(
            state = AddCategoryUiState(
                name = "Groceries"
            ),
            onAction = {},
        )
    }
}