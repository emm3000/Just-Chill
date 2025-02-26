package com.emm.justchill.experiences.supabase

import androidx.compose.animation.Animatable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SupabaseScreen(
    supabasePlayground: SupabasePlayground = koinInject()
) {

    val scope: CoroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            modifier = Modifier.widthIn(min = 150.dp).trackRecompositions(),
            onClick = {
                scope.launch {
                    supabasePlayground.add()
                }
            }
        ) {
            Text(text = "Add")
        }
        OutlinedButton(
            modifier = Modifier.widthIn(min = 150.dp),
            onClick = { /*TODO*/ }
        ) {
            Text(text = "Get")
        }
        OutlinedButton(
            modifier = Modifier.widthIn(min = 150.dp),
            onClick = { /*TODO*/ }
        ) {
            Text(text = "Update")
        }
        OutlinedButton(
            modifier = Modifier.widthIn(min = 150.dp),
            onClick = { /*TODO*/ }
        ) {
            Text(text = "Delete")
        }
    }
}

@Composable
fun Modifier.trackRecompositions(): Modifier {
    // Store recomposition count
    val recompositionCount = remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val flashColor = remember { Animatable(Color.Red) }

    // Use SideEffect to track recompositions (increments only once per recomposition)
    SideEffect {
        recompositionCount.value += 1
    }

    // Draw content with a red border and recomposition count
    return this.then(
        Modifier.drawWithContent {
            drawContent() // Draw the original content
            val text = "Recompositions: ${recompositionCount.value}"
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    textSize = 40f
                    color = android.graphics.Color.RED
                    isAntiAlias = true
                }
                // Draw the recomposition count text below the content
                canvas.nativeCanvas.drawText(
                    text,
                    10f,
                    size.height + 40f,
                    paint
                )
            }
        }
    )
        .border(2.dp, flashColor.value) // Add red border around the component
        .padding(16.dp) // Add padding for space
}

@Preview(showBackground = true)
@Composable
fun SupabaseScreenPreview() {
    EmmTheme {
        SupabaseScreen()
    }
}