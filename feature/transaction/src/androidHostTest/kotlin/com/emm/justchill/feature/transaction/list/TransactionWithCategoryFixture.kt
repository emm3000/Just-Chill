package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.TransactionId
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.domain.transaction.TransactionWithCategory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

internal val FIXTURE_NOON: LocalTime = LocalTime(12, 0)

internal fun transactionFixture(
    id: String,
    type: TransactionType,
    cents: Long,
    month: YearMonth,
    daysIntoMonth: Int = 5,
): TransactionWithCategory = TransactionWithCategory(
    transactionId = TransactionId(id),
    type = type,
    amount = Money(cents),
    description = "movimiento $id",
    occurredAt = LocalDateTime(LocalDate(month.year, month.month, daysIntoMonth), FIXTURE_NOON),
    accountId = AccountId("acc-1"),
    accountName = "BCP",
    category = null,
)
