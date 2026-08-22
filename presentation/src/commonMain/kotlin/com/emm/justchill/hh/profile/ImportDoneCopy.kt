package com.emm.justchill.hh.profile

/**
 * Body copy for the "import finished" notification. Every count inflects, the way
 * `DeleteCategoryCopy` and `ReportShareFormatter` already do it. [transactions] is the one clause
 * never omitted, even at zero; [recurring], [loans] and [loanPayments] each skip their clause at
 * zero rather than naming a table the import did nothing to. The participle drops to singular
 * only when exactly one clause is named and that clause's count is 1 — two or more clauses always
 * take the plural participle.
 */
fun buildImportDoneMessage(transactions: Int, recurring: Int, loans: Int, loanPayments: Int): String {
    val clauses = buildList {
        add(inflect(transactions, "movimiento", "movimientos"))
        if (recurring > 0) add(inflect(recurring, "recurrente", "recurrentes"))
        if (loans > 0) add(inflect(loans, "préstamo", "préstamos"))
        if (loanPayments > 0) add(inflect(loanPayments, "abono", "abonos"))
    }
    val participle = if (clauses.size == 1 && transactions == 1) "importado" else "importados"
    return "Listo — ${joinClauses(clauses)} $participle."
}

private fun inflect(count: Int, singular: String, plural: String): String =
    if (count == 1) "1 $singular" else "$count $plural"

private fun joinClauses(clauses: List<String>): String =
    if (clauses.size <= 1) clauses.joinToString() else "${clauses.dropLast(1).joinToString(", ")} y ${clauses.last()}"
