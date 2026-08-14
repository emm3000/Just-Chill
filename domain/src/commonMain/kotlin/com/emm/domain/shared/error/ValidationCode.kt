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

    /**
     * A snapshot this app uploaded did not survive the round trip — the stored bytes did not match
     * what was sent.
     *
     * **Deliberately not [BackupFileInvalid]**, which is about a file the user CHOSE to restore and
     * whose message says the file is damaged. Here the actor is the same user, the artefact is one
     * this app produced seconds ago, and the event is a backup that failed rather than a restore that
     * was refused. Reusing the other code would tell the owner of a perfectly good ledger that their
     * file is corrupt.
     */
    BackupUploadUnverified,
    Unspecified,
}
