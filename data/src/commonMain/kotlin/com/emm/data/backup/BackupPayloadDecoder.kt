package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal const val SCHEMA_VERSION_KEY = "schemaVersion"

internal fun decodeBackupPayload(json: String): DecodedBackup {
    val root = parse { importJson.parseToJsonElement(json).jsonObject }
    val declaredVersion = schemaVersionOf(root)

    val payload = when (declaredVersion) {
        BACKUP_SCHEMA_VERSION -> parse {
            importJson.decodeFromJsonElement<ExportPayloadDto>(root)
        }

        BACKUP_SCHEMA_VERSION_V2 -> parse {
            importJson.decodeFromJsonElement<ExportPayloadV2Dto>(root).toCurrent()
        }

        BACKUP_SCHEMA_VERSION_V1 -> parse {
            importJson.decodeFromJsonElement<ExportPayloadV1Dto>(root).toCurrent()
        }

        else -> throw DomainException.ValidationError(
            "Unsupported file version.",
            ValidationCode.BackupVersionUnsupported,
        )
    }
    return DecodedBackup(declaredVersion = declaredVersion, payload = payload)
}

// A payload the current build cannot decode is a snapshot that cannot be restored, which is the
// whole answer a verification needs: it walks back to an older pair instead of reporting why this
// one lost. Import keeps the throwing form, where the reason is what the user reads.
@Suppress("SwallowedException")
internal fun decodeBackupPayloadOrNull(json: String): DecodedBackup? = try {
    decodeBackupPayload(json)
} catch (e: DomainException.ValidationError) {
    null
}

private fun schemaVersionOf(root: JsonObject): Int {
    val declared = root[SCHEMA_VERSION_KEY] ?: return BACKUP_SCHEMA_VERSION_V1
    return parse { declared.jsonPrimitive.intOrNull } ?: throw invalidFile(cause = null)
}

private inline fun <T> parse(block: () -> T): T = try {
    block()
} catch (e: SerializationException) {
    throw invalidFile(e)
} catch (e: IllegalArgumentException) {
    throw invalidFile(e)
}

private fun invalidFile(cause: Throwable?) = DomainException.ValidationError(
    "Invalid or corrupted file",
    ValidationCode.BackupFileInvalid,
    cause = cause,
)

private val importJson = Json {
    ignoreUnknownKeys = true
}
