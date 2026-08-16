package com.emm.domain.shared.backup

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Every branch of [toBackupFailureReason], because the mapping is the only thing standing between a
 * named outage and "algo falló".
 *
 * The failure indicator ADR 009 Phase 3 asks for is exactly as useful as this function is accurate:
 * a reason that mislabels a local database read as a network problem sends whoever is holding an
 * outage to the wrong place, and nothing else in the pipeline will contradict it. The inputs below
 * are the real exception shapes `:data` raises — see `BackupFailures.kt`, `DefaultBackupUploader`
 * and `safeDbCall` — not invented ones.
 */
class BackupFailureReasonTest {

    @Test
    fun `a serialization failure is named, not folded into Unknown`() {
        // Stands in for kotlinx.serialization's own SerializationException, which :domain cannot see
        // — only coroutines and datetime are allowed here. The mapping keys on the DomainException
        // type, not on what it wraps, so the stand-in exercises the same branch.
        val failure = DomainException.SerializationError(RuntimeException("bad json"))

        assertEquals(BackupFailureReason.Serialization, failure.toBackupFailureReason())
    }

    /** `SerializationError.cause` is nullable (ADR 009 3a-i); a null one must map the same way. */
    @Test
    fun `a serialization failure with no cause still maps to Serialization`() {
        val failure = DomainException.SerializationError(cause = null, message = "schemaVersion absent")

        assertEquals(BackupFailureReason.Serialization, failure.toBackupFailureReason())
    }

    @Test
    fun `an unreachable network maps to Network`() {
        val failure = DomainException.NetworkUnavailable(RuntimeException("no net"))

        assertEquals(BackupFailureReason.Network, failure.toBackupFailureReason())
    }

    @Test
    fun `a refused or absent session maps to Unauthorized`() {
        val failure = DomainException.Unauthorized("Snapshot backup requested with no session")

        assertEquals(BackupFailureReason.Unauthorized, failure.toBackupFailureReason())
    }

    /**
     * The plan's "hash mismatch". It is distinguishable — `DefaultBackupUploader` raises a
     * `ValidationError` carrying [ValidationCode.BackupUploadUnverified] for both read-back
     * mismatches — and this test is what stops it collapsing back into [BackupFailureReason.Unknown].
     */
    @Test
    fun `an unverified upload maps to Unverified, not Unknown`() {
        val failure = DomainException.ValidationError(
            "Snapshot backup failed: the payload read back does not match the digest its manifest states",
            ValidationCode.BackupUploadUnverified,
        )

        assertEquals(BackupFailureReason.Unverified, failure.toBackupFailureReason())
    }

    /**
     * Every OTHER validation code belongs to a form or to `importFromJson`'s rejection of a file the
     * user chose. Mapping one of those to [BackupFailureReason.Unverified] would tell somebody their
     * upload was corrupt because a different feature refused a different file.
     */
    @Test
    fun `a validation error from anywhere else maps to Unknown`() {
        val failure = DomainException.ValidationError("the file is not a backup", ValidationCode.BackupFileInvalid)

        assertEquals(BackupFailureReason.Unknown, failure.toBackupFailureReason())
    }

    @Test
    fun `a failed local read maps to LocalDatabase`() {
        val failure = DomainException.DatabaseError(RuntimeException("disk I O error"))

        assertEquals(BackupFailureReason.LocalDatabase, failure.toBackupFailureReason())
    }

    /**
     * And the honest one. A Supabase `RestException` — the bucket's 413 for an oversized payload, its
     * 415 for a content type it refuses — reaches here as [DomainException.Unknown] with the status
     * spelled into the message string, so "storage error" and "nobody expected this" are the same
     * value. Naming a `Storage` member would need `:data` to carry the status as data first.
     */
    @Test
    fun `a server refusal is indistinguishable from an unexpected throwable and maps to Unknown`() {
        val serverRefusal = DomainException.Unknown(
            RuntimeException("rest"),
            "The payload could not be uploaded. The server answered HTTP 413: payload too large.",
        )
        val unexpected = DomainException.Unknown(IllegalStateException("nobody saw this coming"))

        assertEquals(BackupFailureReason.Unknown, serverRefusal.toBackupFailureReason())
        assertEquals(BackupFailureReason.Unknown, unexpected.toBackupFailureReason())
    }

    @Test
    fun `a not-found maps to Unknown`() {
        assertEquals(BackupFailureReason.Unknown, DomainException.NotFound("snapshot").toBackupFailureReason())
    }

    // ── The persisted spelling ───────────────────────────────────────────────────────

    @Test
    fun `every reason round-trips through its persisted name`() {
        BackupFailureReason.entries.forEach { reason ->
            assertEquals(reason, BackupFailureReason.fromNameOrNull(reason.name), "$reason did not round-trip")
        }
    }

    /**
     * A device that upgraded across a renamed member reads a name this build no longer has. It must
     * forget which failure it was, never crash: `valueOf` would throw here, inside the code path that
     * exists to REPORT a failure.
     */
    @Test
    fun `an unknown or absent stored name resolves to no reason at all`() {
        assertNull(BackupFailureReason.fromNameOrNull("HashMismatch"))
        assertNull(BackupFailureReason.fromNameOrNull(""))
        assertNull(BackupFailureReason.fromNameOrNull(null))
    }
}
