package com.emm.data.backup

import com.emm.data.shared.ioDispatcher
import com.emm.domain.shared.error.DomainException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.BucketApi
import io.github.jan.supabase.storage.SortOrder
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val BACKUP_BUCKET_ID = "backups"

private const val SESSION_RESOLVE_TIMEOUT_MS = 10_000L

internal class SupabaseBackupObjectStore(private val client: SupabaseClient) : BackupObjectStore {

    override suspend fun ownedPrefix(): String {
        val uid = try {
            withTimeout(SESSION_RESOLVE_TIMEOUT_MS) {
                client.auth.awaitInitialization()
                client.auth.currentUserOrNull()?.id
            }
        } catch (e: TimeoutCancellationException) {
            throw DomainException.NetworkUnavailable(e, SESSION_NEVER_RESOLVED)
        }
        return ownerPrefixOf(uid ?: throw DomainException.Unauthorized(NO_SESSION))
    }

    override suspend fun upload(key: String, bytes: ByteArray) {
        if (!key.endsWith(JSON_EXTENSION)) {
            val defect = IllegalArgumentException("$NOT_JSON: $key")
            throw DomainException.Unknown(defect, "$NOT_JSON: $key.")
        }
        withContext(ioDispatcher) {
            bucket().upload(key, bytes) { upsert = false }
        }
    }

    override suspend fun download(key: String): ByteArray = withContext(ioDispatcher) {
        bucket().downloadAuthenticated(key)
    }

    override suspend fun delete(key: String) {
        withContext(ioDispatcher) {
            bucket().delete(key)
        }
    }

    override suspend fun list(prefix: String, limit: Int, offset: Int): ObjectPage = withContext(ioDispatcher) {
        val page = bucket().list(prefix) {
            this.limit = limit
            this.offset = offset
            sortBy(column = NAME_COLUMN, order = SortOrder.ASC)
        }
        // A row with a null id is not an object: it is the pseudo-row Storage emits to name a folder.
        val (objects, folders) = page.partition { it.id != null }
        ObjectPage(
            names = objects.map { it.name.removePrefix(prefix) },
            serverReturned = page.size,
            folders = folders.map { it.name.removePrefix(prefix) },
        )
    }

    private fun bucket(): BucketApi = client.storage.from(BACKUP_BUCKET_ID)

    private companion object {

        const val JSON_EXTENSION = ".json"

        const val NAME_COLUMN = "name"

        const val NO_SESSION = "Snapshot backup failed: nobody is signed in, so there is no prefix to store it under."

        const val SESSION_NEVER_RESOLVED =
            "Snapshot backup failed: the session did not finish loading within " +
                "${SESSION_RESOLVE_TIMEOUT_MS}ms, so there is no prefix to store it under."

        const val NOT_JSON =
            "Snapshot backup failed: the bucket only accepts application/json and this key does not end in .json"
    }
}
