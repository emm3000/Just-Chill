package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.math.absoluteValue
import kotlin.time.Clock

enum class TransactionsCsvScope { CurrentMonth, Everything }

data class TransactionsCsv(val fileName: String, val content: String)

class ExportTransactionsCsvUseCase(
    private val transactionRepository: TransactionRepository,
    private val clock: Clock,
    private val zone: TimeZone,
) {
    suspend operator fun invoke(scope: TransactionsCsvScope): TransactionsCsv {
        val today: LocalDate = clock.todayIn(zone)
        val movements: List<TransactionWithCategory> = movementsIn(scope, today).first()
        return TransactionsCsv(fileName = fileNameFor(scope, today), content = movements.toCsv())
    }

    private fun movementsIn(scope: TransactionsCsvScope, today: LocalDate): Flow<List<TransactionWithCategory>> =
        when (scope) {
            TransactionsCsvScope.CurrentMonth -> {
                val month: YearMonth = YearMonth.of(today)
                transactionRepository.fetchAllWithCategoryInRange(month.startInclusiveDay(), month.endExclusiveDay())
            }

            TransactionsCsvScope.Everything -> transactionRepository.fetchAllWithCategory()
        }
}

private const val BYTE_ORDER_MARK: String = "\uFEFF"
private const val LINE_END: String = "\r\n"
private const val FIELD_SEPARATOR: String = ","
private const val QUOTE: String = "\""
private const val CENTS_PER_SOL: Long = 100L
private val HEADER: List<String> = listOf("fecha", "tipo", "monto", "cuenta", "categoría", "nota")
private val CHARACTERS_THAT_FORCE_QUOTING: CharArray = charArrayOf(',', '"', '\r', '\n')

private fun fileNameFor(scope: TransactionsCsvScope, today: LocalDate): String {
    val month: YearMonth = YearMonth.of(today)
    val suffix: String = when (scope) {
        TransactionsCsvScope.CurrentMonth -> "${month.year}-${month.month.number.toString().padStart(2, '0')}"
        TransactionsCsvScope.Everything -> "todo-$today"
    }
    return "justchill-movimientos-$suffix.csv"
}

private fun List<TransactionWithCategory>.toCsv(): String {
    val rows: List<List<String>> = sortedWith(compareBy({ it.occurredAt }, { it.transactionId.value }))
        .map(TransactionWithCategory::toCsvFields)
    return BYTE_ORDER_MARK + (listOf(HEADER) + rows).joinToString(separator = "", transform = ::toCsvLine)
}

private fun toCsvLine(fields: List<String>): String =
    fields.joinToString(separator = FIELD_SEPARATOR, transform = ::escapeCsvField) + LINE_END

private fun TransactionWithCategory.toCsvFields(): List<String> = listOf(
    occurredAt.date.toString(),
    type.label,
    amount.toPlainDecimal(),
    accountName,
    category?.name.orEmpty(),
    description,
)

private fun Money.toPlainDecimal(): String {
    val unsigned: Long = cents.absoluteValue
    return "${unsigned / CENTS_PER_SOL}.${(unsigned % CENTS_PER_SOL).toString().padStart(2, '0')}"
}

private fun escapeCsvField(field: String): String = if (field.indexOfAny(CHARACTERS_THAT_FORCE_QUOTING) >= 0) {
    QUOTE + field.replace(QUOTE, QUOTE + QUOTE) + QUOTE
} else {
    field
}
