package com.emm.justchill.hh.presentation.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.emm.justchill.Categories

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesDialog(
    categories: List<Categories> = emptyList(),
    navigateToCreateCategory: () -> Unit = {},
    onDismissRequest: () -> Unit,
) {

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .heightIn(max = screenHeight * 0.8f)
                .clip(RoundedCornerShape(50f))
                .background(Color.White)
                .border(
                    width = 1.dp,
                    color = Color.Black,
                    shape = RoundedCornerShape(50f)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (categories.isEmpty()) {
                    Text(text = "No tienes categorias creadas")
                } else {
                    FlowRow(
                        modifier = Modifier,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        categories.forEachIndexed { _, it ->
                            SuggestionChip(
                                enabled = true,
                                onClick = { },
                                label = { Text(text = it.name) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color.Green
                                )
                            )
                        }
                    }
                }

                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    onClick = navigateToCreateCategory,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.Green
                    )
                ) {
                    Text(text = "Crear")
                }
            }
        }
    }
}