package com.emm.justchill.hh.presentation

sealed class HhRoutes(
    val name: String,
    val route: String,
) {

    data object HhHome: HhRoutes("Inicio", "home")
    data object AddCategory: HhRoutes("Category", "addCategory")
    data object AddTransaction: HhRoutes("Agregar transacción", "addTransaction")
    data object SeeTransaction: HhRoutes("Ver transacciones", "seeTransactions")
}

val hhRoutes = listOf(
    HhRoutes.HhHome,
    HhRoutes.AddTransaction,
    HhRoutes.AddCategory,
    HhRoutes.SeeTransaction,
)