package com.emm.data.backup

import com.emm.data.shared.ioDispatcher
import com.emm.domain.shared.error.DomainException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.BucketApi
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.withContext

/**
 * The bucket the snapshots live in — declared by
 * `supabase/migrations/20260814200043_backup_storage_bucket.sql`, which is the single declaration of
 * its privacy, its 10 MiB ceiling and its `application/json`-only constraint. This constant only
 * names it; it never creates it, and a client that tried to would be the second declaration.
 */
private const val BACKUP_BUCKET_ID = "backups"

/**
 * [BackupObjectStore] over Supabase Storage — the one class in the pipeline that knows a bucket exists.
 *
 * Four traps are already paid for here and are not worth rediscovering; each is argued at length in
 * the migration's own comments, and this is the client half of them.
 *
 * - **The content type must be a bare `application/json`.** storage-api compares the request's
 *   `Content-Type` verbatim against the bucket's `allowed_mime_types` instead of stripping
 *   parameters, so `application/json; charset=utf-8` comes back as HTTP 415 `invalid_mime_type` — a
 *   hard refusal that reads like a server fault. [upload] therefore sets no content type at all and
 *   lets storage-kt fall back to `ContentType.defaultForFilePath(path)`, which yields exactly the
 *   bare form for a key ending in `.json`. **That is a constraint on the KEY, not just on this
 *   file**: every name this store is given must end in `.json`, the manifest sidecar included.
 * - **Never resumable.** `storage.s3_multipart_uploads` and `…_parts` have RLS on with zero
 *   policies, so storage-kt's `uploadAsFlow` fails for `authenticated` with an error that names none
 *   of that. One-shot is the right call under a 10 MiB ceiling anyway; it is simply not a choice.
 * - **Never an upsert.** The bucket grants select, insert and delete and deliberately not update, so
 *   a rewrite is refused by policy. `upsert = false` is storage-kt's default and is written out
 *   anyway, because the value that matters here is the one the server will accept, not the one the
 *   library happens to prefer this version.
 * - **Deletes go through the Storage API and could not go anywhere else.** `storage.objects` carries
 *   a `storage.protect_delete()` trigger that refuses a direct SQL delete whatever the policy says.
 *
 * Nothing here configures a timeout. `Storage.Config.transferTimeout` — 120s, not the 10s
 * `requestTimeout` that bounds Postgrest — is what actually bounds every call this class makes, and
 * ADR 009 leaves making either visible to its own unit. It is named here only so nobody sizes a
 * retry or a chunk against the wrong number.
 */
internal class SupabaseBackupObjectStore(private val client: SupabaseClient) : BackupObjectStore {

    /**
     * Awaits session initialization before reading the user, and the wait is not defensive padding.
     *
     * On Kotlin/Native the persisted session loads asynchronously after the client is built, so
     * `currentUserOrNull()` read too early answers null for a user who is perfectly signed in — the
     * same window `DefaultAuthRepository.awaitSessionInitialization` documents, and the same one that
     * made sync pushes go out with no token. Without the await, the first snapshot after a cold
     * start would report "nobody is signed in" and skip, which is a wrong reason rather than a silent
     * one but still sends whoever reads it to the wrong bug.
     *
     * The await is unbounded on purpose: ADR 009 defers every timeout in this pipeline to its own
     * unit, and inventing a number here would be the third one in the codebase.
     */
    override suspend fun ownedKey(fileName: String): String {
        client.auth.awaitInitialization()
        val uid = client.auth.currentUserOrNull()?.id ?: throw DomainException.Unauthorized(NO_SESSION)
        return "$uid/$fileName"
    }

    override suspend fun upload(key: String, bytes: ByteArray) {
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

    private fun bucket(): BucketApi = client.storage.from(BACKUP_BUCKET_ID)

    private companion object {

        const val NO_SESSION = "Snapshot backup failed: nobody is signed in, so there is no prefix to store it under."
    }
}
