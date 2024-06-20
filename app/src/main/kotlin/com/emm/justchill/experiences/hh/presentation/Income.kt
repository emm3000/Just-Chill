package com.emm.justchill.experiences.hh.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.emm.justchill.core.theme.EmmTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun Income(
    navController: NavController,
    vm: IncomeViewModel = koinViewModel(),
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = "INGRESAR GASTO")

        Spacer(modifier = Modifier.height(20.dp))

        TextFieldWithLabel(
            label = "Cantidad",
            value = vm.mount,
            onChange = vm::updateA
        )
        Spacer(modifier = Modifier.height(15.dp))
        TextFieldWithLabel(
            modifier = Modifier
                .height(140.dp),
            label = "Description",
            value = vm.description,
            onChange = vm::updateB
        )
        Spacer(modifier = Modifier.height(15.dp))
        TextFieldWithLabel(
            label = "Fecha",
            value = vm.date,
            onChange = vm::updateC
        )
        Spacer(modifier = Modifier.height(15.dp))
        TextFieldWithLabel(
            label = "Categoría",
            value = vm.category,
            onChange = vm::updateD
        )
        Spacer(modifier = Modifier.height(15.dp))

        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            onClick = { vm.addTransaction() },
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(text = "GUARDAR")
        }
        Spacer(modifier = Modifier.height(15.dp))
        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            onClick = { navController.navigateUp() },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        ) {
            Text(text = "CANCELAR")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun IncomePreview() {
    EmmTheme {
    }
}