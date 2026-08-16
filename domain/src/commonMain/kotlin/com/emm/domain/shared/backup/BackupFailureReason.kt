package com.emm.domain.shared.backup

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode

/**
 * Why the last snapshot-backup cycle did not produce a recorded snapshot.
 *
 * ADR 009 Phase 3 asks that "every failure logs a distinct reason and increments a visible failure
 * indicator". This is the named half of that: the persisted, UI-readable answer to *which* failure,
 * as opposed to the diagnostic English message on the [DomainException] itself, which is for logs
 * and crash reports and is never shown.
 *
 * ### The members are exactly the distinctions the pipeline can actually make
 *
 * Every member below is produced by a real mapping in [toBackupFailureReason] over a
 * [DomainException] the backup pipeline genuinely throws — the failure taxonomy is derived from
 * `data/.../backup/BackupFailures.kt`, `DefaultBackupUploader` and `safeDbCall`, not from a wish
 * list. A member nothing can populate is worse than a missing one: it puts a reason on a dropdown
 * that no outage will ever select, and the next reader spends their time looking for the code path
 * that raises it.
 *
 * ### What is deliberately NOT a member: a storage error
 *
 * ADR 009's Phase 3 bullet names four reasons — "serialization, network, hash mismatch, storage
 * error". Three of them are here ([Serialization], [Network], [Unverified]). **The fourth is not,
 * because nothing in the pipeline can tell it apart from an unexpected throwable today.** A Supabase
 * `RestException` — the 413 of a payload over the bucket's 10 MiB ceiling, the 415 of a content type
 * it refuses, any 5xx — is mapped by `Throwable.asBackupFailure` to [DomainException.Unknown] with
 * the HTTP status spelled into the *message string*, and so is a name that does not end in `.json`
 * and so is any throwable nobody anticipated. Separating them needs a `DomainException` member that
 * carries the status as data rather than as prose, plus the `:data` mapping to raise it; until that
 * exists, a `Storage` member here would be a label the mapping could only guess at.
 */
enum class BackupFailureReason {

    /**
     * The snapshot could not be turned into (or read back out of) the wire format.
     *
     * From [DomainException.SerializationError], which ADR 009 3a-i introduced for exactly this: the
     * export encode, the manifest encode, and the manifest's own decode of the bytes it describes.
     */
    Serialization,

    /** The network was unreachable or timed out. From [DomainException.NetworkUnavailable]. */
    Network,

    /**
     * The account was refused, gone, or no longer the one the cycle started as.
     *
     * From [DomainException.Unauthorized]: an unauthorized REST refusal, a session that could not be
     * resolved, and the uploader's own mid-cycle account-switch refusal all arrive as this one type
     * and are separated only by their message. That collapse is deliberate — from the outside all
     * three mean "this device is not currently allowed to write that backup".
     */
    Unauthorized,

    /**
     * The snapshot reached the bucket but did not survive the round trip.
     *
     * This is the plan's "hash mismatch", and it **is** distinguishable: `DefaultBackupUploader`
     * raises [DomainException.ValidationError] with [ValidationCode.BackupUploadUnverified] for both
     * read-back mismatches — the payload against its digest, the manifest against its bytes. No
     * other backup failure carries that code.
     *
     * It covers the mismatch whose cleanup ALSO failed, which is the worse of the two endings: the
     * unverified object is still in the bucket. That was not true when this enum first landed — the
     * cleanup delete ran inside the uploader's transport wrapper, so a failed delete threw a network
     * exception and this verdict was never reached, filing the most serious outcome under [Network]
     * or [Unknown]. `DefaultBackupUploader.discarding` is where that was closed, and the two
     * cleanup-failure tests in `DefaultBackupUploaderTest` are what hold it closed.
     */
    Unverified,

    /** The local database could not be read. From [DomainException.DatabaseError] via `safeDbCall`. */
    LocalDatabase,

    /**
     * Anything else, including every deliberate server-side refusal — see the storage-error note on
     * the enum itself for why those are not a member of their own yet.
     */
    Unknown,
    ;

    companion object {

        /**
         * The reason stored under [name], or null when [name] is absent or names nothing.
         *
         * The persisted form of a reason is its enum name, so **renaming a member above silently
         * retires whatever was already stored under the old spelling** — it degrades to "no reason
         * recorded" rather than throwing, which is what `valueOf` would do on a device that upgraded
         * across the rename. A failure indicator that crashes the app it is reporting on is worse
         * than one that forgets which failure it was.
         */
        fun fromNameOrNull(name: String?): BackupFailureReason? = entries.firstOrNull { it.name == name }
    }
}

/**
 * The one mapping from a backup failure to its named reason.
 *
 * Lives here rather than in `:presentation` because it is knowledge about `DomainException`, not
 * about a screen: `:data` raises these types, and the sentence "an unverified upload is a
 * `ValidationError` carrying `BackupUploadUnverified`" has to be spelled in exactly one place or the
 * next consumer will spell it differently.
 */
fun DomainException.toBackupFailureReason(): BackupFailureReason = when (this) {
    is DomainException.SerializationError -> BackupFailureReason.Serialization

    is DomainException.NetworkUnavailable -> BackupFailureReason.Network

    is DomainException.Unauthorized -> BackupFailureReason.Unauthorized

    is DomainException.DatabaseError -> BackupFailureReason.LocalDatabase

    // Only the upload-verification code is a backup reason of its own. Every other validation code
    // belongs to a user-facing form or to `importFromJson`'s untrusted-file rejection, neither of
    // which is a cycle this counter can be counting.
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
