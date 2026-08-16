package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BackupManifestTest {

    @Test
    fun pins_the_field_names_of_the_manifest_itself() {
        val manifest = buildBackupManifest(FILE_NAME, samplePayloadBytes())

        val root = Json.parseToJsonElement(manifest.encodeToJson()).jsonObject

        assertEquals(
            listOf("manifestVersion", "fileName", "payloadSha256", "payloadSchemaVersion", "rowCounts"),
            root.keys.toList(),
        )
        assertEquals(
            listOf("accounts", "categories", "transactions", "recurringMovements"),
            root.getValue("rowCounts").jsonObject.keys.toList(),
        )
    }

    @Test
    fun stamps_its_own_format_version_into_the_file() {
        val root = Json.parseToJsonElement(buildBackupManifest(FILE_NAME, samplePayloadBytes()).encodeToJson())

        assertEquals(BACKUP_MANIFEST_VERSION, root.jsonObject.getValue("manifestVersion").jsonPrimitive.int)
    }

    @Test
    fun refuses_a_manifest_that_declares_no_version_of_its_own() {
        assertFailsWith<SerializationException> {
            Json.decodeFromString<BackupManifestDto>(manifestJsonText(manifestVersion = null))
        }

        assertEquals(
            BACKUP_MANIFEST_VERSION,
            Json.decodeFromString<BackupManifestDto>(manifestJsonText(BACKUP_MANIFEST_VERSION)).manifestVersion,
        )
    }

    @Test
    fun round_trips_through_serialization() {
        val manifest = buildBackupManifest(FILE_NAME, samplePayloadBytes())

        assertEquals(manifest, Json.decodeFromString<BackupManifestDto>(manifest.encodeToJson()))
    }

    @Test
    fun counts_the_rows_of_the_payload_it_describes() {
        val payload = samplePayload()

        val manifest = buildBackupManifest(FILE_NAME, encodePayload(payload))

        assertEquals(payload.accounts.size, manifest.rowCounts.accounts)
        assertEquals(payload.categories.size, manifest.rowCounts.categories)
        assertEquals(payload.transactions.size, manifest.rowCounts.transactions)
        assertEquals(payload.recurringMovements.size, manifest.rowCounts.recurringMovements)
        assertEquals(
            BackupRowCountsDto(accounts = 1, categories = 2, transactions = 3, recurringMovements = 1),
            manifest.rowCounts,
        )
    }

    @Test
    fun hashes_the_exact_bytes_it_was_given() {
        val bytes = samplePayloadBytes()

        assertEquals(sha256Hex(bytes), buildBackupManifest(FILE_NAME, bytes).payloadSha256)
    }

    @Test
    fun carries_the_storage_name_it_was_given() {
        assertEquals(FILE_NAME, buildBackupManifest(FILE_NAME, samplePayloadBytes()).fileName)
    }

    @Test
    fun states_the_version_the_payload_declares() {
        assertEquals(
            BACKUP_SCHEMA_VERSION,
            buildBackupManifest(FILE_NAME, samplePayloadBytes()).payloadSchemaVersion,
        )
    }

    @Test
    fun states_the_declared_version_even_when_it_is_not_the_current_one() {
        val payload = encodePayload(samplePayload()).decodeToString()
            .replace("\"schemaVersion\": $BACKUP_SCHEMA_VERSION", "\"schemaVersion\": 99")

        assertEquals(99, buildBackupManifest(FILE_NAME, payload.encodeToByteArray()).payloadSchemaVersion)
    }

    @Test
    fun refuses_every_payload_it_cannot_describe() {
        UNREADABLE_PAYLOADS.forEach { case ->
            assertFailsWith<DomainException.SerializationError>(message = case.label) {
                buildBackupManifest(FILE_NAME, case.bytes)
            }
        }
    }

    @Test
    fun says_which_way_the_payload_was_unreadable() {
        UNREADABLE_PAYLOADS.forEach { case ->
            val failure = assertFailsWith<DomainException.SerializationError> {
                buildBackupManifest(FILE_NAME, case.bytes)
            }

            assertEquals(
                "Backup payload cannot be described by a manifest: ${case.reason}.",
                failure.message,
                case.label,
            )
        }
    }

    @Test
    fun the_fixture_still_expects_seven_distinct_reasons() {
        val reasons = UNREADABLE_PAYLOADS.map { case -> case.reason }.toSet()

        assertEquals(7, reasons.size, "reasons: $reasons")
    }
}

private class UnreadablePayload(val label: String, val bytes: ByteArray, val reason: String)

private val UNREADABLE_PAYLOADS: List<UnreadablePayload> = listOf(
    UnreadablePayload("truncated UTF-8", byteArrayOf(0xC3.toByte()), "its bytes are not valid UTF-8"),
    UnreadablePayload("not JSON at all", "not a snapshot".encodeToByteArray(), "it is not JSON"),
    UnreadablePayload("root is an array", "[]".encodeToByteArray(), "its root is not a JSON object"),
    UnreadablePayload("no schemaVersion", """{"accounts":[]}""".encodeToByteArray(), "it declares no schemaVersion"),
    UnreadablePayload(
        label = "schemaVersion is an object",
        bytes = """{"schemaVersion":{}}""".encodeToByteArray(),
        reason = "its schemaVersion is not a number",
    ),
    UnreadablePayload(
        label = "schemaVersion is a string",
        bytes = """{"schemaVersion":"three"}""".encodeToByteArray(),
        reason = "its schemaVersion is not a number",
    ),
    UnreadablePayload(
        label = "current version, wrong shape",
        bytes = """{"schemaVersion":$BACKUP_SCHEMA_VERSION}""".encodeToByteArray(),
        reason = "it does not decode as the current snapshot shape " +
            "(it declares schemaVersion $BACKUP_SCHEMA_VERSION)",
    ),
    UnreadablePayload(
        label = "an older version's shape",
        bytes = """{"schemaVersion":1}""".encodeToByteArray(),
        reason = "it does not decode as the current snapshot shape (it declares schemaVersion 1)",
    ),
)

private fun manifestJsonText(manifestVersion: Int?): String {
    val version = manifestVersion?.let { value -> "\"manifestVersion\": $value," }.orEmpty()
    return """
        {
          $version
          "fileName": "$FILE_NAME",
          "payloadSha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
          "payloadSchemaVersion": $BACKUP_SCHEMA_VERSION,
          "rowCounts": { "accounts": 1, "categories": 2, "transactions": 3, "recurringMovements": 1 }
        }
    """.trimIndent()
}

private const val FILE_NAME = "justchill-2026-08-14T03-00-00.json"

private val exportLikeJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

private fun encodePayload(payload: ExportPayloadDto): ByteArray =
    exportLikeJson.encodeToString(payload).encodeToByteArray()

private fun samplePayloadBytes(): ByteArray = encodePayload(samplePayload())

private fun samplePayload() = ExportPayloadDto(
    exportedAt = 1_755_000_000_000,
    appVersion = "v2.4.0",
    accounts = listOf(AccountDto(accountId = "acc-1", name = "Ahorro año", type = "Bank", currency = "PEN")),
    categories = listOf(
        CategoryDto(categoryId = "cat-1", name = "Comida", icon = "🍔", color = "#FF0000", categoryType = "Expense"),
        CategoryDto(categoryId = "cat-2", name = "Sueldo", icon = "💰", color = "#00FF00", categoryType = "Income"),
    ),
    transactions = listOf(
        transaction(id = "tx-1", amountCents = 1_250, description = "Almuerzo", categoryId = "cat-1"),
        transaction(id = "tx-2", amountCents = 300_000, description = "Sueldo de agosto", categoryId = "cat-2"),
        transaction(id = "tx-3", amountCents = 999, description = "Sin categoría", categoryId = null),
    ),
    recurringMovements = listOf(
        RecurringMovementDto(
            recurringMovementId = "rec-1",
            name = "Alquiler",
            type = "Expense",
            amountCents = 90_000,
            description = "Departamento",
            categoryId = "cat-1",
            accountId = "acc-1",
            frequency = "Monthly",
            dayOfMonth = 5,
            isActive = true,
            lastConfirmedPeriod = "2026-07",
            createdAt = 1_750_000_000_000,
        ),
    ),
)

private fun transaction(id: String, amountCents: Long, description: String, categoryId: String?) = TransactionDto(
    transactionId = id,
    type = if (categoryId == "cat-2") "Income" else "Expense",
    amountCents = amountCents,
    description = description,
    occurredAt = "2026-08-10T21:47:33",
    accountId = "acc-1",
    categoryId = categoryId,
)
