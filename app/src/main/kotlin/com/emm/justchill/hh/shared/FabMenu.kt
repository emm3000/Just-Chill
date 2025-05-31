package com.emm.justchill.hh.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.emm.justchill.hh.shared.shared.CategoryRoute

data class FabAction(
    val text: String,
    val onClick: () -> Unit
)

@Composable
fun FabMenu(navController: NavController, showNavBar: Boolean) {

    var fabExpanded by remember { mutableStateOf(false) }

    val fabActions = listOf(
        FabAction("Crear Transacción") {
            navController.navigate(HhRoutes.AddTransaction.route)
            fabExpanded = false
        },
        FabAction("Crear Categorías") {
            navController.navigate(CategoryRoute)
            fabExpanded = false
        },
        FabAction("Crear Cuenta") {
            fabExpanded = false
        }
    )
    AnimatedVisibility(
        visible = !showNavBar,
        enter = fadeIn(animationSpec = tween(durationMillis = 200)),
        exit = fadeOut(animationSpec = tween(durationMillis = 200))
    ) {
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.End,
        ) {
            fabActions.forEach { action ->
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                            .clickable(onClick = action.onClick)
                    ) {
                        Text(
                            text = action.text,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.shapes.small
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                }
            }
            FloatingActionButton(
                modifier = Modifier,
                onClick = { fabExpanded = !fabExpanded },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                val a: Float by animateFloatAsState(
                    targetValue = if (fabExpanded) 45f else 0f,
                )
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Añadir",
                    modifier = Modifier.size(24.dp)
                        .rotate(a)
                )
            }
        }
    }
}