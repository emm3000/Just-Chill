package com.emm.justchill.feature.profile

internal fun importDoneText(transactions: Int, recurring: Int, loans: Int, loanPayments: Int): String {
    val clauses: List<String> = buildList {
        add(countClause(transactions, "movimiento", "movimientos"))
        if (recurring > 0) add(countClause(recurring, "recurrente", "recurrentes"))
        if (loans > 0) add(countClause(loans, "préstamo", "préstamos"))
        if (loanPayments > 0) add(countClause(loanPayments, "abono", "abonos"))
    }
    val participle: String = if (clauses.size == 1 && transactions == 1) "importado" else "importados"
    return "Listo — ${joinedClauses(clauses)} $participle."
}

internal fun countClause(count: Int, singular: String, plural: String): String =
    if (count == 1) "1 $singular" else "$count $plural"

private fun joinedClauses(clauses: List<String>): String =
    if (clauses.size <= 1) clauses.joinToString() else "${clauses.dropLast(1).joinToString(", ")} y ${clauses.last()}"
