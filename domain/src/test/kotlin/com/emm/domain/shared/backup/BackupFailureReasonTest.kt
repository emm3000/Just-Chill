package com.emm.domain.shared.backup

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BackupFailureReasonTest {

    @Test
    fun `a serialization failure is named, not folded into Unknown`() {
        val failure = DomainException.SerializationError(RuntimeException("bad json"))

        assertEquals(BackupFailureReason.Serialization, failure.toBackupFailureReason())
    }

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

    @Test
    fun `an unverified upload maps to Unverified, not Unknown`() {
        val failure = DomainException.ValidationError(
            "Snapshot backup failed: the payload read back does not match the digest its manifest states",
            ValidationCode.BackupUploadUnverified,
        )

        assertEquals(BackupFailureReason.Unverified, failure.toBackupFailureReason())
    }

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

    @Test
    fun `a server refusal is named, not folded in with the unexpected throwable beside it`() {
        val serverRefusal = DomainException.RemoteRejected(
            "The payload could not be uploaded. The server answered HTTP 413: payload too large.",
            statusCode = 413,
            cause = RuntimeException("rest"),
        )
        val unexpected = DomainException.Unknown(IllegalStateException("nobody saw this coming"))

        assertEquals(BackupFailureReason.RemoteRejected, serverRefusal.toBackupFailureReason())
        assertEquals(BackupFailureReason.Unknown, unexpected.toBackupFailureReason())
    }

    @Test
    fun `a not-found maps to Unknown`() {
        assertEquals(BackupFailureReason.Unknown, DomainException.NotFound("snapshot").toBackupFailureReason())
    }

    @Test
    fun `every reason round-trips through its persisted name`() {
        BackupFailureReason.entries.forEach { reason ->
            assertEquals(reason, BackupFailureReason.fromNameOrNull(reason.name), "$reason did not round-trip")
        }
    }

    @Test
    fun `an unknown or absent stored name resolves to no reason at all`() {
        assertNull(BackupFailureReason.fromNameOrNull("HashMismatch"))
        assertNull(BackupFailureReason.fromNameOrNull(""))
        assertNull(BackupFailureReason.fromNameOrNull(null))
    }
}
