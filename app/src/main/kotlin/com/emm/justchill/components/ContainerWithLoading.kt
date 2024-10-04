package com.emm.justchill.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints

@Composable
fun ContainerWithLoading(
    isLoading: Boolean,
    content: @Composable () -> Unit,
) {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        content()
        Layout(
            content = content,
            modifier = Modifier,
        ) { measurable: List<Measurable>, constraints: Constraints ->
            layout(
                width = 1,
                height = 2
            ) {

            }
        }
        if (isLoading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Gray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}