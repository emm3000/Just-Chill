package com.emm.justchill.hh.shared.shared

import kotlinx.serialization.Serializable

sealed interface Screen {

    @Serializable
    data class EditTransaction(val transactionId: String) : Screen

    @Serializable
    object Category : Screen

    @Serializable
    object Dashboard : Screen

    @Serializable
    object Login : Screen
}


