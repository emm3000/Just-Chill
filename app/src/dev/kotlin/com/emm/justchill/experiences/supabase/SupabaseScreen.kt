package com.emm.justchill.experiences.supabase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            modifier = Modifier.widthIn(min = 150.dp),
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

@Preview(showBackground = true)
@Composable
fun SupabaseScreenPreview() {
    EmmTheme {
        SupabaseScreen()
    }
}