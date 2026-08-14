package com.emm.justchill.hh.profile

/**
 * Body copy for the "import finished" notification.
 *
 * [recurring] joined the count in the same commit that made the v3 import restore that table
 * (`69f28de5`); until then the sentence only ever named `transactions`. `recurring == 0` keeps
 * that original sentence byte for byte — a v1/v2 file, or a v3 file whose owner has no templates,
 * must not start naming a table it did nothing to. Only `recurring > 0` grows the sentence.
 *
 * The `transactions` side's missing singular (`"1 movimientos importados"` reads wrong) is
 * pre-existing and deliberately NOT fixed here — out of scope for the change that added
 * [recurring]. Do not "fix" it as a drive-by.
 */
fun buildImportDoneMessage(transactions: Int, recurring: Int): String {
    if (recurring == 0) return "Listo — $transactions movimientos importados."
    val recurringPhrase = if (recurring == 1) "1 recurrente" else "$recurring recurrentes"
    return "Listo — $transactions movimientos y $recurringPhrase importados."
}
