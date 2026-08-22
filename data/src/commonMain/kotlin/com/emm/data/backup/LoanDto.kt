package com.emm.data.backup

import com.emm.data.shared.toOccurredAtOrNull
import com.emm.data.shared.toOccurredAtText
import com.emm.domain.loan.Loan
import com.emm.domain.shared.LoanId
import com.emm.domain.shared.Money
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

internal const val BACKUP_LOANS_SINCE_VERSION: Int = 4

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
