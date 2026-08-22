package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal const val BACKUP_MANIFEST_VERSION: Int = 1

@Serializable
internal data class BackupManifestDto(
    val manifestVersion: Int,
    val fileName: String,
    val payloadSha256: String,
    val payloadSchemaVersion: Int,
    val rowCounts: BackupRowCountsDto,
)

@Serializable
internal data class BackupRowCountsDto(
    val accounts: Int,
    val categories: Int,
    val transactions: Int,
    val recurringMovements: Int,
    val loans: Int,
    val loanPayments: Int,
)

internal fun buildBackupManifest(fileName: String, payloadBytes: ByteArray): BackupManifestDto {
    val payloadText = readingPayload(NOT_UTF8) { payloadBytes.decodeToString(throwOnInvalidSequence = true) }
    val parsed = readingPayload(NOT_JSON) { payloadJson.parseToJsonElement(payloadText) }
    val root = readingPayload(ROOT_NOT_AN_OBJECT) { parsed.jsonObject }
    val declaredVersion = declaredVersionOf(root)

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
            loans = payload.loans.size,
            loanPayments = payload.loanPayments.size,
        ),
    )
}

internal fun BackupManifestDto.encodeToJson(): String = manifestJson.encodeToString(this)

internal fun decodeBackupManifestOrNull(bytes: ByteArray): BackupManifestDto? {
    val manifest: BackupManifestDto? = readingManifest {
        manifestJson.decodeFromString<BackupManifestDto>(bytes.decodeToString(throwOnInvalidSequence = true))
    }
    return manifest?.takeIf { it.manifestVersion == BACKUP_MANIFEST_VERSION }
}

/**
 * A manifest this build cannot read is not a verdict on the payload beside it — the only caller
 * walks back to an older pair — so the reason it was unreadable is discarded rather than reported.
 */
@Suppress("SwallowedException")
private inline fun <T> readingManifest(block: () -> T): T? = try {
    block()
} catch (e: CharacterCodingException) {
    null
} catch (e: SerializationException) {
    null
} catch (e: IllegalArgumentException) {
    null
}

private fun declaredVersionOf(root: JsonObject): Int {
    val declared = root[SCHEMA_VERSION_KEY] ?: throw payloadUnreadable(NO_VERSION_KEY, cause = null)
    return readingPayload(VERSION_NOT_A_NUMBER) { declared.jsonPrimitive.intOrNull }
        ?: throw payloadUnreadable(VERSION_NOT_A_NUMBER, cause = null)
}

private val manifestJson = Json { prettyPrint = true }

private val payloadJson = Json

private const val NOT_UTF8 = "its bytes are not valid UTF-8"
private const val NOT_JSON = "it is not JSON"
private const val ROOT_NOT_AN_OBJECT = "its root is not a JSON object"
private const val NO_VERSION_KEY = "it declares no $SCHEMA_VERSION_KEY"
private const val VERSION_NOT_A_NUMBER = "its $SCHEMA_VERSION_KEY is not a number"
private const val WRONG_SHAPE = "it does not decode as the current snapshot shape"

private inline fun <T> readingPayload(reason: String, block: () -> T): T = try {
    block()
} catch (e: CharacterCodingException) {
    throw payloadUnreadable(reason, e)
} catch (e: SerializationException) {
    throw payloadUnreadable(reason, e)
} catch (e: IllegalArgumentException) {
    throw payloadUnreadable(reason, e)
}

private fun payloadUnreadable(reason: String, cause: Throwable?): DomainException = DomainException.SerializationError(
    cause = cause,
    message = "Backup payload cannot be described by a manifest: $reason.",
)
