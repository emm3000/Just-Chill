package com.emm.domain.shared.error

/**
 * Machine-readable reason for a [DomainException.ValidationError].
 *
 * The exception's `message` stays English: it is for logs, stack traces and crash reports.
 * The UI must map this code to a user-facing string and must never surface `message` — that is
 * exactly what made 24 English validation strings reach the Spanish snackbar.
 *
 * [Unspecified] is the default so a new call site compiles, but it degrades to a generic message.
 * Prefer adding a case here over shipping [Unspecified].
 */
enum class ValidationCode {
    NameRequired,
    AccountRequired,
    AmountRequired,
    AmountMustBePositive,
    DateInTheFuture,
    DayOfMonthOutOfRange,
    RecurringAlreadyConfirmed,
    AccountHasTransactions,
    AccountHasRecurringMovements,
    EmailInvalid,
    EmailAlreadyRegistered,
    PasswordRequired,
    PasswordTooShort,
    PasswordTooWeak,
    PasswordUnchanged,
    GoogleTokenInvalid,
    BackupFileInvalid,
    BackupVersionUnsupported,
    Unspecified,
}
