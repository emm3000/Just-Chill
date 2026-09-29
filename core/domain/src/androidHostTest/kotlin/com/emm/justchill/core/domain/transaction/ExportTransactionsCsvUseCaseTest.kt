package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.TransactionId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class ExportTransactionsCsvUseCaseTest {

    private val lima: TimeZone = TimeZone.of("America/Lima")
    private val lastNightOfSeptemberInLima: Clock = fixedClock("2026-10-01T04:30:00Z")

    private val transactionRepository: TransactionRepository = mockk {
        every { fetchAllWithCategoryInRange(any(), any()) } returns flowOf(emptyList())
        every { fetchAllWithCategory() } returns flowOf(emptyList())
    }

    private val groceries: Category = Category(
        categoryId = CategoryId("cat-food"),
        name = "Comida",
        icon = "food",
        color = "green",
        categoryType = CategoryType.Spend,
    )

    private fun movement(
        id: String,
        occurredAt: LocalDateTime,
        amount: Money = Money(1_00L),
        type: TransactionType = TransactionType.Spend,
        category: Category? = groceries,
        description: String = "",
    ): TransactionWithCategory = TransactionWithCategory(
        transactionId = TransactionId(id),
        type = type,
        amount = amount,
        description = description,
        occurredAt = occurredAt,
        accountId = AccountId("acc-cash"),
        accountName = "Efectivo",
        category = category,
    )

    private suspend fun exportCurrentMonthOf(vararg movements: TransactionWithCategory): List<String> {
        every { transactionRepository.fetchAllWithCategoryInRange(any(), any()) } returns flowOf(movements.toList())
        val useCase = ExportTransactionsCsvUseCase(
            transactionRepository,
            lastNightOfSeptemberInLima,
            lima,
            Dispatchers.Unconfined,
        )
        return useCase(TransactionsCsvScope.CurrentMonth).content.removeSuffix("\r\n").split("\r\n").drop(1)
    }

    private fun exportUseCase(): ExportTransactionsCsvUseCase =
        ExportTransactionsCsvUseCase(transactionRepository, lastNightOfSeptemberInLima, lima, Dispatchers.Unconfined)

    private fun fixedClock(instant: String): Clock = object : Clock {
        override fun now(): Instant = Instant.parse(instant)
    }

    @Test
    fun `an empty month yields the byte order mark and the header only`() = runTest {
        val useCase = ExportTransactionsCsvUseCase(
            transactionRepository,
            lastNightOfSeptemberInLima,
            lima,
            Dispatchers.Unconfined,
        )

        val csv: TransactionsCsv = useCase(TransactionsCsvScope.CurrentMonth)

        assertEquals("\uFEFFfecha,tipo,monto,cuenta,categoría,nota\r\n", csv.content)
    }

    @Test
    fun `the current month is the one the injected clock reads in the injected zone`() = runTest {
        val useCase = ExportTransactionsCsvUseCase(
            transactionRepository,
            lastNightOfSeptemberInLima,
            lima,
            Dispatchers.Unconfined,
        )

        val csv: TransactionsCsv = useCase(TransactionsCsvScope.CurrentMonth)

        assertEquals("justchill-movimientos-2026-09.csv", csv.fileName)
        verify { transactionRepository.fetchAllWithCategoryInRange("2026-09-01", "2026-10-01") }
    }

    @Test
    fun `rows go oldest first and break a tie on the transaction id`() = runTest {
        val rows: List<String> = exportCurrentMonthOf(
            movement("tx-c", LocalDateTime(2026, Month.SEPTEMBER, 20, 9, 0), description = "tercero"),
            movement("tx-b", LocalDateTime(2026, Month.SEPTEMBER, 3, 9, 0), description = "segundo"),
            movement("tx-a", LocalDateTime(2026, Month.SEPTEMBER, 3, 9, 0), description = "primero"),
        )

        assertEquals(listOf("primero", "segundo", "tercero"), rows.map { it.substringAfterLast(',') })
    }

    @Test
    fun `a row carries the ISO date, the type label, the unsigned amount, the account, the category and the note`() =
        runTest {
            val rows: List<String> = exportCurrentMonthOf(
                movement(
                    id = "tx-1",
                    occurredAt = LocalDateTime(2026, Month.SEPTEMBER, 25, 18, 40),
                    amount = Money(123456L),
                    type = TransactionType.Income,
                    description = "sueldo",
                ),
            )

            assertEquals(listOf("2026-09-25,Ingreso,1234.56,Efectivo,Comida,sueldo"), rows)
        }

    @Test
    fun `five cents keep their leading zeros`() = runTest {
        val rows: List<String> = exportCurrentMonthOf(
            movement("tx-1", LocalDateTime(2026, Month.SEPTEMBER, 1, 8, 0), amount = Money(5L)),
        )

        assertEquals("0.05", rows.single().split(',')[2])
    }

    @Test
    fun `a movement with no category leaves the field empty`() = runTest {
        val rows: List<String> = exportCurrentMonthOf(
            movement("tx-1", LocalDateTime(2026, Month.SEPTEMBER, 1, 8, 0), category = null, description = "taxi"),
        )

        assertEquals("2026-09-01,Gasto,1.00,Efectivo,,taxi", rows.single())
    }

    @Test
    fun `a note holding a comma, a quote and a line break is quoted with its quote doubled`() = runTest {
        every { transactionRepository.fetchAllWithCategoryInRange(any(), any()) } returns flowOf(
            listOf(
                movement(
                    id = "tx-1",
                    occurredAt = LocalDateTime(2026, Month.SEPTEMBER, 1, 8, 0),
                    description = "pan, \"el bueno\"\r\ny leche",
                ),
            ),
        )
        val useCase = ExportTransactionsCsvUseCase(
            transactionRepository,
            lastNightOfSeptemberInLima,
            lima,
            Dispatchers.Unconfined,
        )

        val csv: TransactionsCsv = useCase(TransactionsCsvScope.CurrentMonth)

        assertEquals(
            "\uFEFFfecha,tipo,monto,cuenta,categoría,nota\r\n" +
                "2026-09-01,Gasto,1.00,Efectivo,Comida,\"pan, \"\"el bueno\"\"\r\ny leche\"\r\n",
            csv.content,
        )
    }

    @Test
    fun `everything reads every movement and names the file after the injected day`() = runTest {
        every { transactionRepository.fetchAllWithCategory() } returns flowOf(
            listOf(movement("tx-1", LocalDateTime(2024, Month.JANUARY, 5, 8, 0), description = "antiguo")),
        )
        val useCase = ExportTransactionsCsvUseCase(
            transactionRepository,
            lastNightOfSeptemberInLima,
            lima,
            Dispatchers.Unconfined,
        )

        val csv: TransactionsCsv = useCase(TransactionsCsvScope.Everything)

        assertEquals("justchill-movimientos-todo-2026-09-30.csv", csv.fileName)
        assertEquals("2024-01-05,Gasto,1.00,Efectivo,Comida,antiguo\r\n", csv.content.substringAfter("nota\r\n"))
    }
}
