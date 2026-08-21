package com.emm.data.loan

import com.emm.data.BalancesByPerson
import com.emm.data.Loans
import com.emm.data.shared.toOccurredAtOrNull
import com.emm.domain.loan.Loan
import com.emm.domain.loan.PersonBalance
import com.emm.domain.loan.remaining
import com.emm.domain.shared.LoanId
import com.emm.domain.shared.Money

fun Loans.asEntity() = LoanEntity(
    loanId = loanId,
    personName = personName,
    personKey = personKey,
    principal = principal,
    interestBps = interestBps,
    totalDue = totalDue,
    note = note,
    lentAt = lentAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<Loans>.asEntity() = map(Loans::asEntity)

fun LoanEntity.asExternalModelOrNull(): Loan? {
    val parsedLentAt = lentAt.toOccurredAtOrNull() ?: return null
    return Loan(
        id = LoanId(loanId),
        personName = personName,
        personKey = personKey,
        principal = Money(principal),
        interestBps = interestBps.toInt(),
        totalDue = Money(totalDue),
        note = note,
        lentAt = parsedLentAt,
    )
}

fun List<LoanEntity>.asExternalModel() = mapNotNull(LoanEntity::asExternalModelOrNull)

fun BalancesByPerson.asExternalModel() = PersonBalance(
    personKey = personKey,
    personName = personName,
    remaining = remaining(totalDue = Money(totalDue), paidSoFar = Money(paidSoFar)),
)
