// The file is named for the format version it freezes, not for the one type that version needed —
// `BackupV1.kt` sets that convention and escapes this rule only by accident, because v1 happened to
// need two types. Renaming this to ExportPayloadV2Dto.kt would put two spellings of the same idea
// side by side in one package, and every reference to it ("see BackupV2.kt") reads worse for it.
// Inline rather than in config/detekt/baseline-data-main.xml on purpose: the baseline inventories
// debt that already exists, and putting brand-new code in it hides a deliberate choice among the
// things nobody has got to yet.
@file:Suppress("MatchingDeclarationName")

package com.emm.data.backup

import kotlinx.serialization.Serializable

/**
 * The backup payload shape at schema version 2, frozen ahead of the version that grows past it.
 *
 * Nothing reads this yet. `decodePayload` still decodes version 2 through the live [ExportPayloadDto]
 * because the two are the same shape and [BACKUP_SCHEMA_VERSION] is still 2. This type is here so
 * that the commit which adds `recurringMovements` and moves the number can wire a v2 branch in the
 * same breath, instead of shipping a window where version 2 has no reader — which is the whole
 * ordering the KDoc on [BACKUP_SCHEMA_VERSION] describes.
 *
 * **This has to keep working forever**, for the same reason [ExportPayloadV1Dto] does: backup files
 * are the data escape hatch, they live in storage the user chose, outside the app, beyond the reach
 * of any schema migration. Version 2 is additionally the version every file that exists today was
 * written in.
 *
 * The failure this prevents is the loud one, not a silent misread. `decodePayload` dispatches on the
 * version the file *declares*, so once `BACKUP_SCHEMA_VERSION` moves to 3 a file saying `2` matches
 * no branch at all and is refused as `BackupVersionUnsupported` — surfacing as *"El archivo está
 * dañado o no es un respaldo de JustChill"*, the app telling the owner their intact backup is not a
 * backup. That is the same failure [ExportPayloadV1Dto] exists to prevent, and the reason a frozen
 * reader has to exist before the number moves rather than after.
 *
 * It carries a second job once the current payload gains recurring movements and the import starts
 * sweeping that table: this branch is what keeps *"the file has nothing to say about them"* apart
 * from *"the file says there are none"*. A v2 file has no recurring movements to give and never did,
 * so restoring one must leave the templates already on the device alone — nothing older than v3 can
 * put them back. `BackupV2CompatibilityTest` pins exactly that.
 *
 * ### The coupling this type deliberately accepts
 *
 * The three item lists below hold the **live** [AccountDto], [CategoryDto] and [TransactionDto], not
 * copies frozen at v2 — exactly as [ExportPayloadV1Dto] shares its accounts and categories. Only the
 * payload shell is frozen here, because only the shell is what the next version changes: it gains a
 * list, and no item shape moves with it.
 *
 * The cost is real, and it is not the same for all three:
 *
 *  - [AccountDto] and [CategoryDto] are shared by **v1, v2 and the current payload**. Editing either
 *    changes how every backup file ever written is read, and turns both `BackupV1CompatibilityTest`
 *    and `BackupV2CompatibilityTest` red.
 *  - [TransactionDto] is shared by **v2 and the current payload only** — v1 froze its own
 *    [TransactionV1Dto], and its fixture carries `"date"` rather than `"occurredAt"`. Editing it
 *    turns `BackupV2CompatibilityTest` red and leaves the v1 suite green, correctly.
 *
 * The coupling is tolerable only because it is caught rather than assumed: both suites restore
 * hand-written literal JSON, so a rename or a removed field fails a test before it can reach a real
 * file. Copying all three DTOs into every frozen version instead would trade one loud failure for
 * three shapes drifting apart in silence.
 *
 * If a future version does change an item shape, freeze that item the way [TransactionV1Dto] was
 * frozen and point this payload at the frozen copy.
 */
@Serializable
internal data class ExportPayloadV2Dto(
    /**
     * Defaulted exactly as v2's own DTO defaulted it.
     *
     * Unlike v1's default this one is not load-bearing — a file with no `schemaVersion` key routes
     * to v1, never here, so the default is never the value that gets used. It is kept because this
     * type's job is to be the shape v2 actually had, and a reader that quietly differs from the
     * writer is the whole class of bug the frozen types exist to remove.
     */
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION_V2,
    val exportedAt: Long,
    val appVersion: String,
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
)

/**
 * A literal, never `BACKUP_SCHEMA_VERSION - 1` or anything else derived from the current version.
 *
 * `decodePayload` picks its reader by comparing the declared version against constants like this
 * one, so a derived value would follow the current version upward and re-point a frozen branch at a
 * shape it was never written for. The number 2 is a fact about files already on disk; it cannot move.
 *
 * It equals [BACKUP_SCHEMA_VERSION] today, which is exactly why the v2 branch is not wired yet: two
 * equal branch conditions in the same `when` is a collision, not a reader. The next commit separates
 * them by moving [BACKUP_SCHEMA_VERSION], not this.
 */
internal const val BACKUP_SCHEMA_VERSION_V2: Int = 2

/**
 * Reads a v2 payload as the current one.
 *
 * A field-for-field copy while the two shapes are still identical. It exists now so the conversion
 * has one home the moment the current payload grows a field version 2 never had — and what it will
 * then carry across is the *absence*: a v2 file contributes no recurring movements, and the import
 * has to read that as "this file has nothing to say about them" rather than as "this file says there
 * are none".
 */
internal fun ExportPayloadV2Dto.toCurrent(): ExportPayloadDto = ExportPayloadDto(
    schemaVersion = BACKUP_SCHEMA_VERSION,
    exportedAt = exportedAt,
    appVersion = appVersion,
    accounts = accounts,
    categories = categories,
    transactions = transactions,
)
