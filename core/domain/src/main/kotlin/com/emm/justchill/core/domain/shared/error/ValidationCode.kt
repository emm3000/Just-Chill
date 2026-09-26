package com.emm.justchill.core.domain.shared.error

enum class ValidationCode {
    NameRequired,
    AccountRequired,
    AmountRequired,
    AmountMustBePositive,
    DateInTheFuture,
    AccountHasTransactions,
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
    TotalBelowPaid,
    Unspecified,
}
