package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Version of the MANIFEST format — the sidecar's own shape, not the snapshot's.
 *
 * It is born at 1 and versioned from the first commit for the reason [BACKUP_SCHEMA_VERSION] states
 * at length: a format that ships unversioned cannot be extended later without a reader that has to
 * guess which shape it is holding, and "the key is missing" then becomes indistinguishable from "the
 * value is absent". The snapshot format learned that once; this one starts where that one ended up.
 *
 * **This number moves in the same commit that changes [BackupManifestDto], never in an earlier one**
 * — same rule, same reason.
 */
internal const val BACKUP_MANIFEST_VERSION: Int = 1

/**
 * The sidecar that says what a stored snapshot is supposed to be.
 *
 * Uploaded next to the payload (ADR 009 Phase 2b): the payload is verified against
 * [payloadSha256] after read-back, and [rowCounts] is what makes a truncated-but-well-formed file
 * visible — a snapshot that parses and simply lost half its rows passes every check the JSON itself
 * can offer.
 *
 * **Two version numbers live here and they are different numbers.** [manifestVersion] is this
 * file's own format; [payloadSchemaVersion] is the snapshot's. They can move independently and
 * usually will. Everything describing the snapshot carries the `payload` prefix — `payloadSha256`,
 * `payloadSchemaVersion` — and the one field that describes this file does not, so a call site
 * reading either name in isolation still knows which artefact it is talking about.
 *
 * The field names are a wire format from the moment the first manifest is uploaded: renaming one
 * later breaks every stored manifest, so `BackupManifestSerializationTest` pins them.
 */
@Serializable
internal data class BackupManifestDto(
    val manifestVersion: Int = BACKUP_MANIFEST_VERSION,
    /** Storage name of the payload this manifest describes. Naming is ADR 009 Phase 2c's decision. */
    val fileName: String,
    /** Lowercase hex, 64 chars — [sha256Hex] over the exact bytes uploaded. */
    val payloadSha256: String,
    /** The `schemaVersion` the payload DECLARES. Not [manifestVersion]; see the class KDoc. */
    val payloadSchemaVersion: Int,
    val rowCounts: BackupRowCountsDto,
)

/**
 * Rows per table, named exactly as the arrays they count in [ExportPayloadDto] — a manifest count
 * and the payload key it describes are the same word on purpose.
 */
@Serializable
internal data class BackupRowCountsDto(
    val accounts: Int,
    val categories: Int,
    val transactions: Int,
    val recurringMovements: Int,
)

/**
 * The manifest for [payloadBytes], derived from nothing else.
 *
 * **The counts come from the payload, never from a second read of the database**, and that is the
 * point of taking bytes as the only input. A manifest built from its own query pass describes the
 * database at the instant it ran, which is not the instant the snapshot was taken — the two can
 * legitimately disagree, and then the integrity check fires on a file that is perfectly intact. A
 * manifest derived from the bytes is a claim about the bytes and can only be wrong in the same way
 * they are, which is exactly what a verification wants: hash, version and counts all describe one
 * byte sequence, and no call site can pair the counts of one snapshot with the digest of another.
 *
 * [payloadSchemaVersion] is read from the raw JSON before the payload is deserialized, the same
 * discipline `DefaultBackupRepository.decodePayload` documents: [ExportPayloadDto.schemaVersion]
 * carries a default, so a file that declares no version at all would decode as the current one and
 * the manifest would state a version the file never claimed.
 *
 * Anything unreadable here is a defect in this app's own export rather than a bad user file — these
 * bytes were produced by [DefaultBackupRepository.exportToJson] moments earlier. It still fails with
 * a NAMED reason instead of a raw [SerializationException], because ADR 009's hard constraint 4
 * forbids a silent failure anywhere in the backup pipeline and an unnamed one is how the 2026-08-12
 * outage stayed invisible.
 */
internal fun buildBackupManifest(fileName: String, payloadBytes: ByteArray): BackupManifestDto = readingPayload {
    val root = payloadJson.parseToJsonElement(payloadBytes.decodeToString()).jsonObject
    val declaredVersion = root[SCHEMA_VERSION_KEY]?.jsonPrimitive?.intOrNull ?: throw payloadUnreadable(cause = null)
    val payload = payloadJson.decodeFromJsonElement<ExportPayloadDto>(root)

    BackupManifestDto(
        fileName = fileName,
        payloadSha256 = sha256Hex(payloadBytes),
        payloadSchemaVersion = declaredVersion,
        rowCounts = BackupRowCountsDto(
            accounts = payload.accounts.size,
            categories = payload.categories.size,
            transactions = payload.transactions.size,
            recurringMovements = payload.recurringMovements.size,
        ),
    )
}

/**
 * The manifest as the bytes that get uploaded.
 *
 * `encodeDefaults` is load-bearing rather than cosmetic: [BackupManifestDto.manifestVersion] is a
 * defaulted property, and a `Json` without it writes a manifest carrying no version field — an
 * unversioned file claiming to be the versioned format. It is stated here, once, so no future
 * upload path has to remember it.
 */
internal fun BackupManifestDto.encodeToJson(): String = manifestJson.encodeToString(this)

private val manifestJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

/**
 * Strict on purpose — no `ignoreUnknownKeys`, unlike the import reader.
 *
 * The import reader tolerates surprises because it is handed a file of unknown provenance. This one
 * reads bytes this same build wrote seconds ago, so a key it does not recognise is not an old file,
 * it is a bug — and tolerating it would produce a manifest that silently describes something other
 * than what shipped.
 */
private val payloadJson = Json

@Suppress("SwallowedException")
private inline fun <T> readingPayload(block: () -> T): T = try {
    block()
} catch (e: SerializationException) {
    throw payloadUnreadable(e)
} catch (e: IllegalArgumentException) {
    throw payloadUnreadable(e)
}

private fun payloadUnreadable(cause: Throwable?) = DomainException.ValidationError(
    "Backup payload could not be read back as a snapshot; no manifest can describe it.",
    ValidationCode.BackupFileInvalid,
    cause = cause,
)
