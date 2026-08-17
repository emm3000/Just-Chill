package com.emm.domain.shared.backup

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode

enum class BackupFailureReason {

    Serialization,

    Network,

    Unauthorized,

    Busy,

    Unverified,

    LocalDatabase,

    Unknown,
    ;

    companion object {

        fun fromNameOrNull(name: String?): BackupFailureReason? = entries.firstOrNull { it.name == name }
    }
}

fun DomainException.toBackupFailureReason(): BackupFailureReason = when (this) {
    is DomainException.SerializationError -> BackupFailureReason.Serialization

    is DomainException.NetworkUnavailable -> BackupFailureReason.Network

    is DomainException.Unauthorized -> BackupFailureReason.Unauthorized

    is DomainException.Busy -> BackupFailureReason.Busy

    is DomainException.DatabaseError -> BackupFailureReason.LocalDatabase

    is DomainException.ValidationError ->
        if (code == ValidationCode.BackupUploadUnverified) {
            BackupFailureReason.Unverified
        } else {
            BackupFailureReason.Unknown
        }

    is DomainException.NotFound,
    is DomainException.Unknown,
    -> BackupFailureReason.Unknown
}
