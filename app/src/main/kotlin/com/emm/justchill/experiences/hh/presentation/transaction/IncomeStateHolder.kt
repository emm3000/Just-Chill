package com.emm.justchill.experiences.hh.presentation.transaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

@Composable
fun rememberIncomeStateHolder(): IncomeStateHolder {
    val rememberCoroutineScope = rememberCoroutineScope()
    return remember {
        IncomeStateHolder(rememberCoroutineScope)
    }
}

@Stable
class IncomeStateHolder(
    viewModelScope: CoroutineScope,
) {

    var mount by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    var date by mutableStateOf("")
        private set

    var category by mutableStateOf("")
        private set

    var isEnabled by mutableStateOf(false)
        private set

    init {
        combine(
            snapshotFlow { mount },
            snapshotFlow { description },
            snapshotFlow { date },
            snapshotFlow { category },
        ) { mount, description, date, category ->
            isEnabled = mount.isNotEmpty()
                    && description.isNotEmpty()
                    && date.isNotEmpty()
                    && category.isNotEmpty()

        }.launchIn(viewModelScope)
    }
}