package com.emm.justchill.hh.shared.shared

import kotlinx.serialization.Serializable

sealed interface Screen {

    @Serializable
    data class EditTransaction(val transactionId: String) : Screen

    @Serializable
    data object Category : Screen

    @Serializable
    data object Dashboard : Screen

    @Serializable
    data object PreLogin : Screen

    @Serializable
    data object Login : Screen

    @Serializable
    data object Register : Screen
}


