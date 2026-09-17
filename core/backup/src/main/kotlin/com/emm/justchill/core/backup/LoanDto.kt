package com.emm.justchill.core.backup

import com.emm.justchill.core.backup.shared.toOccurredAtOrNull
import com.emm.justchill.core.backup.shared.toOccurredAtText
import com.emm.justchill.core.domain.loan.Loan
import com.emm.justchill.core.domain.shared.LoanId
import com.emm.justchill.core.domain.shared.Money
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

const val BACKUP_LOANS_SINCE_VERSION: Int = 4

@Serializable
data class LoanDto(
    val loanId: String,
    val personName: String,
    val personKey: String,
    val principalCents: Long,
    val interestBps: Int,
    val totalDueCents: Long,
    val note: String,
    val lentAt: String,
)

fun Loan.toDto() = LoanDto(
    loanId = id.value,
    personName = personName,
    personKey = personKey,
    principalCents = principal.cents,
    interestBps = interestBps,
    totalDueCents = totalDue.cents,
    note = note,
    lentAt = lentAt.toOccurredAtText(),
)

fun LoanDto.toEntityOrNull(): Loan? {
    val parsedLentAt: LocalDateTime = lentAt.toOccurredAtOrNull() ?: return null
    return Loan(
        id = LoanId(loanId),
        personName = personName,
        personKey = personKey,
        principal = Money(principalCents),
        interestBps = interestBps,
        totalDue = Money(totalDueCents),
        note = note,
        lentAt = parsedLentAt,
    )
}
