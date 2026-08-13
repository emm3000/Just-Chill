package com.emm.data.backup

import kotlinx.serialization.Serializable

/**
 * Schema version 3: the payload carries `recurringMovements`, the templates the format ignored for
 * its first two versions.
 *
 * Version 2 was where a transaction's `occurredAt` (ISO local text) replaced version 1's `date`
 * (epoch millis). Both older shapes are frozen — `BackupV1.kt`, `BackupV2.kt` — and both still
 * restore; see `DefaultBackupRepository.decodePayload` for the dispatch.
 *
 * **This number moves in the same commit that changes the shape below it, never in an earlier one.**
 * Export stamps it into every file it writes, so bumping it ahead of the fields ships files that
 * declare a format they were not written in — and the next version's reader cannot tell them apart
 * from the real thing, because a list the file never had decodes as an empty one rather than as
 * nothing. On a device holding real accumulated data, "the file says there are none" is what
 * destroys the rows.
 *
 * The order that avoids it: freeze the outgoing shape into its own `BackupV<n>.kt` with its own
 * literal constant first (`BackupV1.kt`, `BackupV2.kt`), then add the new fields and move this
 * number together, then wire the frozen branch in `decodePayload`.
 */
const val BACKUP_SCHEMA_VERSION: Int = 3

@Serializable
data class ExportPayloadDto(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val exportedAt: Long,
    val appVersion: String,
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
    /**
     * Required, with **no default**, and that is a behaviour rather than a style choice.
     *
     * A file that declares version 3 and carries no `recurringMovements` key was not written by this
     * app. Without a default it fails deserialization and is refused as
     * `ValidationCode.BackupFileInvalid` — a malformed file, reported as one. Give it a default and
     * that same file decodes as an empty list, which a destructive sweep would then act on: "there
     * are none" and "the key is missing" are not the same claim, and only one of them is safe to
     * delete rows over.
     *
     * **The rule that follows from it, for whoever writes the sweep: gate on the DECLARED version,
     * never on this list being empty.** A device with no templates exports version 3 with an empty
     * array, and a version 1 or 2 file arrives here with an empty array too — supplied by its own
     * frozen reader's `toCurrent()`, which can state the absence because it knows the format never
     * carried one. Emptiness cannot tell those two apart. The version can, and it is the only thing
     * that can, which is why the number had to move in the same commit as this field.
     */
    val recurringMovements: List<RecurringMovementDto>,
)
