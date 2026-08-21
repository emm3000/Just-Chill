package com.emm.domain.shared.error

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

    BackupUploadUnverified,
    PersonRequired,
    InterestOutOfRange,
    PaymentExceedsBalance,
    Unspecified,
}
