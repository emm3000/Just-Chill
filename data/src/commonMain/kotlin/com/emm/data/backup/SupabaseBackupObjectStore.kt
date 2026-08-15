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

/**
 * The bucket the snapshots live in — declared by
 * `supabase/migrations/20260814200043_backup_storage_bucket.sql`, which is the single declaration of
 * its privacy, its 10 MiB ceiling and its `application/json`-only constraint. This constant only
 * names it; it never creates it, and a client that tried to would be the second declaration.
 */
private const val BACKUP_BUCKET_ID = "backups"

/**
 * How long [SupabaseBackupObjectStore.ownedPrefix] waits for a persisted session to finish loading.
 *
 * **Backup declares its own rather than reaching for `DefaultSyncRepository`'s identical constant**,
 * which is private to the row-replication engine ADR 009 Phase 5 deletes: importing it would make the
 * backup pipeline break on the day that file is removed, for a shared `10_000L` and nothing else. The
 * number is deliberately the same, because the thing being waited on is the same
 * (`Auth.awaitInitialization`) and two different ceilings for one wait would be a worse answer than
 * one duplicated literal (`docs/CODE_QUALITY.md`: duplication of text, not of knowledge).
 *
 * Unbounded was the previous answer and it was wrong. ADR 009 defers `requestTimeout` and
 * `transferTimeout` — HTTP knobs on an in-flight call — and says nothing about session resolution,
 * which is not an HTTP call at all. A wait that never ends holds ADR 009 Phase 2c's `launchOp`
 * concurrent-op guard with no exception and no message, which is hard constraint 4's exact shape.
 */
private const val SESSION_RESOLVE_TIMEOUT_MS = 10_000L

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
 *   file**: every key handed to [upload] must end in `.json`, the manifest sidecar included, and
 *   [upload] enforces it rather than asking to be trusted.
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
 * Nothing here configures an HTTP timeout. `Storage.Config.transferTimeout` — 120s, not the 10s
 * `requestTimeout` that bounds Postgrest — is what actually bounds every REQUEST this class makes,
 * and ADR 009 leaves making either visible to its own unit. It is named here only so nobody sizes a
 * retry or a chunk against the wrong number. [SESSION_RESOLVE_TIMEOUT_MS] is a different thing
 * entirely and is set here: it bounds a wait for a local session to load, which no HTTP knob reaches.
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
     * **The wait is bounded, and a session that never resolves is a named failure** rather than a
     * suspension nobody can see — [SESSION_RESOLVE_TIMEOUT_MS] carries why, including why the number
     * is copied from the sync engine instead of imported from it. It maps to
     * [DomainException.NetworkUnavailable] because that is what an unresolvable session is from here:
     * something retryable that has not happened yet, not a refusal.
     */
    override suspend fun ownedPrefix(): String {
        val uid = try {
            withTimeout(SESSION_RESOLVE_TIMEOUT_MS) {
                client.auth.awaitInitialization()
                client.auth.currentUserOrNull()?.id
            }
        } catch (e: TimeoutCancellationException) {
            throw DomainException.NetworkUnavailable(e, SESSION_NEVER_RESOLVED)
        }
        return "${uid ?: throw DomainException.Unauthorized(NO_SESSION)}/"
    }

    /**
     * **The `.json` check is here and not in a KDoc**, because the KDoc above used to call it a hard
     * requirement while nothing held anyone to it. The manifest sidecar is safe by construction; a
     * payload name is whatever ADR 009 Phase 2c decides to pass, and a name that ends any other way
     * makes storage-kt derive a content type the bucket refuses — HTTP 415 `invalid_mime_type`, a
     * server-shaped error for a client-side naming defect.
     *
     * It throws [DomainException.Unknown] rather than a `ValidationError`: nothing about it is user
     * input, it can only be a defect in this app's own naming, and `Unknown`'s user-facing message is
     * the honest one for that. The diagnostic message names the key so the defect is one grep away.
     */
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

    /**
     * **The limit, the offset and the sort order are all set explicitly, because all three have
     * server-side defaults this code does not control.** storage-kt sends only the fields the filter
     * block sets, so an unset limit means whatever the Storage API decides that day — a number a BOM
     * bump can move without a compile error, and the caller's paging arithmetic is built on knowing
     * it. The V1 list route this posts to declares `limit` with `minimum: 1` and no maximum and
     * passes it through unclamped (read out of storage-api's own source), so what is asked for is
     * what bounds the page.
     *
     * Sorting by name ascending is the same order as chronologically ascending, since every snapshot
     * name carries its own UTC stamp. Under the data-relative retention policy that is also the
     * *safest* order for a page boundary to fall in — an early page holds the oldest objects, so
     * anything a short read would have missed is newer than everything it saw. The caller pages to
     * completion regardless; the deterministic order is what makes two consecutive pages join up
     * instead of overlapping arbitrarily — **for a static bucket.** Offset paging over a listing that
     * is *mutating between the two requests* can instead skip: if an object sorting before the
     * boundary is deleted after the first request and before the second, everything after it shifts
     * left by one, and the entry that was going to be first on the second page is never returned by
     * either. A skipped sidecar makes its payload an orphan, which the prune deletes on sight. This
     * needs more than a thousand objects in the bucket (this class's own page size) plus a concurrent
     * deleter to land inside that window, which `launchOp`'s concurrent-op guard and the single-device
     * premise both push toward remote rather than absent — it is not fixed here, and a fix would be
     * cursor-based paging, not this offset scheme.
     *
     * **Folder entries are dropped here — but they are counted.** Supabase Storage derives folders
     * from the `/` delimiter and returns them in a listing as a `FileObject` whose fields are all
     * null except the name, so `pinned` appears beside real objects. A folder is not something
     * [delete] could act on, and the prune must never scan under `pinned/` at all. It still occupied
     * a place in the page the server returned, so it is included in [ObjectPage.serverReturned] and
     * excluded from [ObjectPage.names]; conflating the two is what let a full page read as the last
     * one.
     *
     * **The prefix is stripped defensively.** [BackupObjectStore.list]'s contract is relative names,
     * and the V1 list endpoint answers with names relative to the prefix it was given —
     * `removePrefix` is a no-op against that behaviour and the guard that keeps `prefix + name` from
     * becoming `<uid>/<uid>/…` if it ever were not. It is here rather than in the caller because
     * this is the class that knows what the wire said.
     */
    override suspend fun list(prefix: String, limit: Int, offset: Int): ObjectPage = withContext(ioDispatcher) {
        val page = bucket().list(prefix) {
            this.limit = limit
            this.offset = offset
            sortBy(column = NAME_COLUMN, order = SortOrder.ASC)
        }
        ObjectPage(
            names = page.filter { it.id != null }.map { it.name.removePrefix(prefix) },
            serverReturned = page.size,
        )
    }

    private fun bucket(): BucketApi = client.storage.from(BACKUP_BUCKET_ID)

    private companion object {

        const val JSON_EXTENSION = ".json"

        /** One of the four columns the list endpoint accepts; the others are the object's timestamps. */
        const val NAME_COLUMN = "name"

        const val NO_SESSION = "Snapshot backup failed: nobody is signed in, so there is no prefix to store it under."

        const val SESSION_NEVER_RESOLVED =
            "Snapshot backup failed: the session did not finish loading within " +
                "${SESSION_RESOLVE_TIMEOUT_MS}ms, so there is no prefix to store it under."

        const val NOT_JSON =
            "Snapshot backup failed: the bucket only accepts application/json and this key does not end in .json"
    }
}
