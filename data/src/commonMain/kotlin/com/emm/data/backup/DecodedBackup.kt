package com.emm.data.backup

/**
 * A backup file after `decodePayload` has read it: the payload in the CURRENT shape, plus the format
 * version the FILE ITSELF declared.
 *
 * The two fields say different things and neither can be recovered from the other.
 *
 *  - [payload] is genuinely current. Every frozen reader ends in a `toCurrent()` that restamps
 *    [ExportPayloadDto.schemaVersion] to [BACKUP_SCHEMA_VERSION], because after the conversion the
 *    object really is the current shape and must not keep claiming to be the one it arrived as.
 *  - [declaredVersion] is the fact the restamp destroys: which shape the bytes on disk were written
 *    in. It is [BACKUP_SCHEMA_VERSION_V1], [BACKUP_SCHEMA_VERSION_V2] or [BACKUP_SCHEMA_VERSION] —
 *    the exact value `decodePayload` dispatched on, never a value derived from [payload].
 *
 * **This type exists so the import can tell "the file has nothing to say about recurring movements"
 * apart from "the file says there are none".** After `toCurrent()` a v1 file, a v2 file and a v3 file
 * exported from a device with no templates are identical objects: `schemaVersion = 3`,
 * `recurringMovements = []`. A destructive sweep that read that emptiness as a statement would
 * tombstone every template on the device the moment the owner restored an older backup — and no v1
 * or v2 file can put them back, because the format never carried them.
 *
 * [declaredVersion] is the only thing that separates the three, which is why it is carried out of the
 * decoder rather than left inside it. Whoever writes the version-gated sweep gates on this field.
 */
internal data class DecodedBackup(
    val declaredVersion: Int,
    val payload: ExportPayloadDto,
)
