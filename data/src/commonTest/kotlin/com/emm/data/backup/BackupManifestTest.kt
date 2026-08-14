package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The manifest is a WIRE FORMAT from the first upload: every field name here is stored in a file
 * this app will later have to read, so a rename is a compatibility break and not a refactor. These
 * tests are what makes that break visible in a diff.
 */
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
    fun writes_its_own_version_even_though_the_property_is_defaulted() {
        // `encodeDefaults` in the manifest's own Json is what puts this key in the file. Without it
        // the versioned format would ship its very first file carrying no version at all.
        val root = Json.parseToJsonElement(buildBackupManifest(FILE_NAME, samplePayloadBytes()).encodeToJson())

        assertEquals(BACKUP_MANIFEST_VERSION, root.jsonObject.getValue("manifestVersion").jsonPrimitive.int)
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
        // The manifest DESCRIBES bytes; it does not validate them. Reading the version off the raw
        // JSON rather than off the decoded DTO is what makes that possible — `schemaVersion` carries
        // a default, so a decoded payload always looks current whatever the file said.
        val payload = encodePayload(samplePayload()).decodeToString()
            .replace("\"schemaVersion\": $BACKUP_SCHEMA_VERSION", "\"schemaVersion\": 99")

        assertEquals(99, buildBackupManifest(FILE_NAME, payload.encodeToByteArray()).payloadSchemaVersion)
    }

    @Test
    fun refuses_a_payload_that_declares_no_version() {
        // Not a hypothetical guard against a user file — these bytes come from this app's own export
        // seconds earlier, so this is the defect path, and it fails with a named reason rather than
        // producing a manifest that invents a version the file never claimed.
        val failure = assertFailsWith<DomainException.ValidationError> {
            buildBackupManifest(FILE_NAME, """{"accounts":[]}""".encodeToByteArray())
        }

        assertEquals(ValidationCode.BackupFileInvalid, failure.code)
    }

    @Test
    fun refuses_bytes_that_are_not_json_at_all() {
        val failure = assertFailsWith<DomainException.ValidationError> {
            buildBackupManifest(FILE_NAME, "not a snapshot".encodeToByteArray())
        }

        assertEquals(ValidationCode.BackupFileInvalid, failure.code)
    }

    @Test
    fun refuses_json_that_is_missing_a_required_payload_field() {
        val failure = assertFailsWith<DomainException.ValidationError> {
            buildBackupManifest(FILE_NAME, """{"schemaVersion":3}""".encodeToByteArray())
        }

        assertEquals(ValidationCode.BackupFileInvalid, failure.code)
    }
}

private const val FILE_NAME = "justchill-2026-08-14T03-00-00.json"

/** The same configuration `DefaultBackupRepository.exportToJson` writes real snapshots with. */
private val exportLikeJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

private fun encodePayload(payload: ExportPayloadDto): ByteArray =
    exportLikeJson.encodeToString(payload).encodeToByteArray()

private fun samplePayloadBytes(): ByteArray = encodePayload(samplePayload())

/**
 * Accents on purpose — "Ahorro año", "categoría" — so the bytes hashed here are the multi-byte UTF-8
 * the real Spanish ledger produces rather than an ASCII stand-in.
 */
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
