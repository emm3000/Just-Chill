package com.emm.data.backup

import com.emm.domain.shared.backup.BackupEraser
import com.emm.domain.shared.error.DomainException
import io.github.jan.supabase.SupabaseClient
import kotlin.coroutines.cancellation.CancellationException

class DefaultBackupEraser internal constructor(private val store: BackupObjectStore) : BackupEraser {

    constructor(client: SupabaseClient) : this(SupabaseBackupObjectStore(client))

    override suspend fun eraseOwnedBackups(userId: String) = asNotErased {
        val prefix: String = ownedPrefixFor(userId)
        val failed: List<String> = deleteEach(keysUnder(prefix, depth = 0))
        if (failed.isNotEmpty()) throw notErased(failed)
        refuseIfAnythingSurvives(prefix)
    }

    // Every refusal reaches the UI as one message naming that the account survived, so no failure
    // inside the sweep may leave here wearing another type.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun asNotErased(sweep: suspend () -> Unit) = try {
        sweep()
    } catch (e: CancellationException) {
        throw e
    } catch (e: DomainException.BackupsNotErased) {
        throw e
    } catch (e: Exception) {
        throw DomainException.BackupsNotErased(failedCount = 0, message = e.message ?: SWEEP_BROKE, cause = e)
    }

    private suspend fun ownedPrefixFor(userId: String): String {
        val prefix: String = storageCall(PREFIX_UNRESOLVED) { store.ownedPrefix() }
        val owner: String = ownerPrefixOf(userId)
        if (prefix != owner) throw ownerChanged(owner, prefix)
        return prefix
    }

    private suspend fun keysUnder(prefix: String, depth: Int): List<String> {
        if (depth > MAX_FOLDER_DEPTH) throw tooDeep(prefix)
        val listing: BucketListing = store.wholeBucket(prefix, LIST_FAILED, LISTING_NEVER_ENDED)
        val here: List<String> = listing.names.map { prefix + it }
        val nested: List<String> = listing.folders.flatMap { keysUnder(childPrefix(prefix, it), depth + 1) }
        return here + nested
    }

    // A failed delete is collected rather than fatal: this is the last moment anything can reach
    // these objects, so the sweep removes as many as it can before it refuses.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun deleteEach(keys: List<String>): List<String> {
        val failed = mutableListOf<String>()
        keys.forEach { key ->
            try {
                store.delete(key)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed += "$key could not be deleted: ${e.message ?: e::class.simpleName}"
            }
        }
        return failed
    }

    // A delete answering 200 does not assert the row is gone, and an object written after the
    // listing was never in it — only a second listing that comes back empty proves the prefix is.
    private suspend fun refuseIfAnythingSurvives(prefix: String) {
        val page: ObjectPage = storageCall(SURVIVOR_CHECK_FAILED) { store.list(prefix, BACKUP_LIST_PAGE_SIZE, 0) }
        if (page.serverReturned != 0) throw survivorsRemain(page.serverReturned)
    }
}

private fun childPrefix(prefix: String, folder: String): String = prefix + folder.removeSuffix("/") + "/"

private fun notErased(failed: List<String>): DomainException = DomainException.BackupsNotErased(
    failedCount = failed.size,
    cause = IllegalStateException(failed.joinToString(separator = "; ")),
)

private fun survivorsRemain(remaining: Int): DomainException = DomainException.BackupsNotErased(
    failedCount = remaining,
    message = "$ERASE_FAILED $remaining objects were still listed under the account prefix once the " +
        "sweep finished, so the account was not deleted.",
)

private fun ownerChanged(owner: String, live: String): DomainException = DomainException.BackupsNotErased(
    failedCount = 0,
    message = "$OWNER_CHANGED it targets $owner and the live session owns $live, so nothing was listed or deleted.",
)

private fun tooDeep(prefix: String): DomainException = DomainException.BackupsNotErased(
    failedCount = 0,
    message = "$ERASE_FAILED $prefix nests deeper than $MAX_FOLDER_DEPTH folders, so the tree cannot be read " +
        "completely. Erasing a partial listing would report an empty prefix that is not empty, so nothing " +
        "was deleted.",
)

private const val MAX_FOLDER_DEPTH = 5

private const val ERASE_FAILED = "Account backup erase failed:"

private const val SWEEP_BROKE = "$ERASE_FAILED the sweep broke, so the account was not deleted."

private const val OWNER_CHANGED = "$ERASE_FAILED the signed-in account is not the one being deleted:"

private const val PREFIX_UNRESOLVED = "$ERASE_FAILED the owning prefix could not be resolved."

private const val LIST_FAILED = "$ERASE_FAILED the bucket could not be listed, so nothing was deleted."

private const val SURVIVOR_CHECK_FAILED =
    "$ERASE_FAILED the account prefix could not be listed again, so it cannot be shown to be empty."

private const val LISTING_NEVER_ENDED = "$ERASE_FAILED the listing never returned an empty page within " +
    "$BACKUP_LIST_MAX_PAGES pages of $BACKUP_LIST_PAGE_SIZE objects, so it cannot be read completely. " +
    "Erasing a partial listing would report an empty prefix that is not empty, so nothing was deleted."
