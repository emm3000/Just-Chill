package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
 *
 * It is stamped by [buildBackupManifest] and nowhere else, because
 * [BackupManifestDto.manifestVersion] deliberately carries no default: see its own KDoc.
 */
internal const val BACKUP_MANIFEST_VERSION: Int = 1

/**
 * The sidecar that says what a stored snapshot is supposed to be.
 *
 * Uploaded next to the payload (ADR 009 Phase 2b): the payload is verified against
 * [payloadSha256] after read-back.
 *
 * **Two version numbers live here and they are different numbers.** [manifestVersion] is this
 * file's own format; [payloadSchemaVersion] is the snapshot's. They can move independently and
 * usually will. Everything describing the snapshot carries the `payload` prefix — `payloadSha256`,
 * `payloadSchemaVersion` — and the one field that describes this file does not, so a call site
 * reading either name in isolation still knows which artefact it is talking about.
 *
 * The field names are a wire format from the moment the first manifest is uploaded: renaming one
 * later breaks every stored manifest, so `BackupManifestTest` pins them.
 */
@Serializable
internal data class BackupManifestDto(
    /**
     * Required, with **no default** — the same construction, for the same reason, as
     * [ExportPayloadDto.recurringMovements].
     *
     * A default would make this format commit the exact mistake the doc above claims it avoided:
     * a manifest carrying no `manifestVersion` key would decode as version 1 rather than being
     * refused, so a file written by something that is not this app — or by a future writer that
     * dropped the field — would be read under the rules of a version it never declared. Without the
     * default it fails deserialization, which is the honest answer, and it removes the need for the
     * `encodeDefaults` that a defaulted property would have required on the way out.
     */
    val manifestVersion: Int,
    /** Storage name of the payload this manifest describes. Naming is ADR 009 Phase 2c's decision. */
    val fileName: String,
    /** Lowercase hex, 64 chars — [sha256Hex] over the exact bytes uploaded. */
    val payloadSha256: String,
    /** The `schemaVersion` the payload DECLARES. Not [manifestVersion]; see the class KDoc. */
    val payloadSchemaVersion: Int,
    /**
     * Rows per table in the payload — for a HUMAN and for comparing snapshots, never as a second
     * opinion about the bytes.
     *
     * **It cannot corroborate [payloadSha256], and whoever writes the read-back check must not treat
     * it as if it could.** Both are derived from the same byte array by [buildBackupManifest], so on
     * read-back they are tautologically consistent: if the digest matches, the counts match by
     * construction and prove nothing further; if it does not, the file is already known to be wrong
     * and the counts describe a payload that no longer exists. Nor do they catch a truncated
     * snapshot — truncated bytes never reach here, because the strict decode below refuses them
     * outright rather than counting what survived.
     *
     * What they are actually for: reading a bucket without downloading it. "Yesterday's snapshot
     * held 812 movements and today's holds 3" is a question the manifest can answer at listing time,
     * and it is the shape a silent local data loss takes. And before a restore, "this file holds N
     * movements" is something a person can be shown and can refuse.
     */
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
 * legitimately disagree, and then the numbers stored beside a perfectly intact file are simply
 * wrong. A manifest derived from the bytes is a claim about the bytes and can only be wrong in the
 * same way they are, so no call site can pair the counts of one snapshot with the digest of another.
 *
 * [BackupManifestDto.payloadSchemaVersion] is read from the raw JSON before the payload is
 * deserialized, the same discipline `DefaultBackupRepository.decodePayload` documents:
 * [ExportPayloadDto.schemaVersion] carries a default, so a file that declares no version at all
 * would decode as the current one and the manifest would state a version the file never claimed.
 *
 * Anything unreadable here is a defect in this app's own export rather than a bad user file — these
 * bytes were produced by [DefaultBackupRepository.exportToJson] moments earlier, and
 * [DefaultBackupUploader.upload] is [buildBackupManifest]'s only caller, always fed that same
 * fresh export and never an arbitrary file a user picked. That is why every failure below is
 * [DomainException.SerializationError] and never `ValidationCode.BackupFileInvalid` — that code is
 * reserved for `DefaultBackupRepository.importFromJson`, which decodes bytes off disk this app did
 * not just write and cannot vouch for. **Each way it can fail says which one happened**, because
 * ADR 009's hard constraint 4 forbids a silent failure anywhere in the backup pipeline, and one
 * message shared by every cause is silent in the only sense that matters: the log tells whoever is
 * holding the outage nothing it did not already know.
 */
internal fun buildBackupManifest(fileName: String, payloadBytes: ByteArray): BackupManifestDto {
    // `throwOnInvalidSequence` because this whole unit's thesis is that the bytes are the bytes: the
    // default repairs malformed UTF-8 into U+FFFD, which would let a manifest describe a payload
    // nobody can decode back. Unreachable today; free to close.
    val payloadText = readingPayload(NOT_UTF8) { payloadBytes.decodeToString(throwOnInvalidSequence = true) }
    // Parsing and shape are two steps because they are two answers: "this is not JSON" and "this is
    // JSON, but the root is an array" send a reader looking in different places.
    val parsed = readingPayload(NOT_JSON) { payloadJson.parseToJsonElement(payloadText) }
    val root = readingPayload(ROOT_NOT_AN_OBJECT) { parsed.jsonObject }
    val declaredVersion = declaredVersionOf(root)

    // The declared version is interpolated into the reason on purpose: it is what separates "a v1 or
    // v2 file reached the manifest builder" from "the current shape gained a key this build has
    // never seen", and both arrive here as the same SerializationException.
    val payload = readingPayload("$WRONG_SHAPE (it declares $SCHEMA_VERSION_KEY $declaredVersion)") {
        payloadJson.decodeFromJsonElement<ExportPayloadDto>(root)
    }

    return BackupManifestDto(
        manifestVersion = BACKUP_MANIFEST_VERSION,
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
 * No `encodeDefaults`: nothing in [BackupManifestDto] or [BackupRowCountsDto] has a default, so
 * every field is written because every field is required. That is the point — the setting existed
 * only to compensate for a defaulted [BackupManifestDto.manifestVersion], and removing the default
 * removed the need for the compensation rather than moving it somewhere quieter.
 */
internal fun BackupManifestDto.encodeToJson(): String = manifestJson.encodeToString(this)

/**
 * The version the payload declares, or a named failure — absent and unusable are DIFFERENT answers.
 *
 * Folding them together is how a `"schemaVersion": "three"` would have been logged as "declares no
 * schemaVersion", which sends whoever is reading that line looking for the wrong bug. Same split,
 * for the same reason, as `DefaultBackupRepository.schemaVersionOf`.
 */
private fun declaredVersionOf(root: JsonObject): Int {
    val declared = root[SCHEMA_VERSION_KEY] ?: throw payloadUnreadable(NO_VERSION_KEY, cause = null)
    // `jsonPrimitive` THROWS when the value is an object or an array, so it has to be read inside
    // the guard; `intOrNull` covers the value that is a primitive but not a number.
    return readingPayload(VERSION_NOT_A_NUMBER) { declared.jsonPrimitive.intOrNull }
        ?: throw payloadUnreadable(VERSION_NOT_A_NUMBER, cause = null)
}

private val manifestJson = Json { prettyPrint = true }

/**
 * Strict on purpose — no `ignoreUnknownKeys`, unlike the import reader.
 *
 * The import reader tolerates surprises because it is handed a file of unknown provenance. This one
 * reads bytes this same build wrote seconds ago, so a key it does not recognise is not an old file,
 * it is a bug — and tolerating it would produce a manifest that silently describes something other
 * than what shipped.
 */
private val payloadJson = Json

// One reason per thing a log reader would do differently on seeing it. `schemaVersion` present as an
// object and present as a string share [VERSION_NOT_A_NUMBER] deliberately — the field is unusable
// either way and the next move is the same. They stay distinguishable in a log, but by whether a
// `cause` is attached rather than by what it says: the object and array shapes carry an
// `IllegalArgumentException` from the element read, while string, boolean, float and JSON-null carry
// no cause at all. An earlier version of this comment claimed the cause identified which shape it
// was; it does not, and nothing should be built on that.
private const val NOT_UTF8 = "its bytes are not valid UTF-8"
private const val NOT_JSON = "it is not JSON"
private const val ROOT_NOT_AN_OBJECT = "its root is not a JSON object"
private const val NO_VERSION_KEY = "it declares no $SCHEMA_VERSION_KEY"
private const val VERSION_NOT_A_NUMBER = "its $SCHEMA_VERSION_KEY is not a number"
private const val WRONG_SHAPE = "it does not decode as the current snapshot shape"

@Suppress("SwallowedException")
private inline fun <T> readingPayload(reason: String, block: () -> T): T = try {
    block()
} catch (e: CharacterCodingException) {
    throw payloadUnreadable(reason, e)
} catch (e: SerializationException) {
    throw payloadUnreadable(reason, e)
} catch (e: IllegalArgumentException) {
    throw payloadUnreadable(reason, e)
}

/**
 * [DomainException.SerializationError], never `ValidationError(BackupFileInvalid)` — see the class
 * KDoc's "Anything unreadable here..." paragraph for why this file's bytes are never an untrusted
 * user file.
 *
 * [cause] is null only at the two call sites that read [SCHEMA_VERSION_KEY] and find it absent or
 * not a number: nothing threw there, the value was simply judged unusable. A [SerializationException]
 * carrying [reason] is synthesized for those so [DomainException.SerializationError] — which, like
 * [DomainException.Unknown] and [DomainException.NetworkUnavailable], always carries a real cause —
 * still gets one, rather than the site inventing its own ad hoc null-cause exception.
 */
private fun payloadUnreadable(reason: String, cause: Throwable?): DomainException = DomainException.SerializationError(
    cause = cause ?: SerializationException(reason),
    message = "Backup payload cannot be described by a manifest: $reason.",
)
