package com.emm.justchill.hh.profile

/**
 * Body copy for the "import finished" notification.
 *
 * [recurring] joined the count in the same commit that made the v3 import restore that table
 * (`69f28de5`); until then the sentence only ever named `transactions`. `recurring == 0` still
 * names nothing else — a v1/v2 file, or a v3 file whose owner has no templates, must not start
 * naming a table it did nothing to. Only `recurring > 0` grows the sentence.
 *
 * Both counts inflect, the way `DeleteCategoryCopy` and `ReportShareFormatter` already do it. The
 * `transactions` side did not until a device showed the result: `(1, 1)` rendered
 * `"1 movimientos y 1 recurrente importados."` — the two rules side by side in one sentence, which
 * is what made a long-standing wrong plural finally unignorable.
 *
 * The participle only drops to `"importado"` in the `recurring == 0` singular branch. Two singular
 * subjects joined by "y" take a plural participle, so `(1, 1)` keeps `"importados"`.
 */
fun buildImportDoneMessage(transactions: Int, recurring: Int): String {
    val transactionsPhrase = if (transactions == 1) "1 movimiento" else "$transactions movimientos"
    if (recurring == 0) {
        val participle = if (transactions == 1) "importado" else "importados"
        return "Listo — $transactionsPhrase $participle."
    }
    val recurringPhrase = if (recurring == 1) "1 recurrente" else "$recurring recurrentes"
    return "Listo — $transactionsPhrase y $recurringPhrase importados."
}
