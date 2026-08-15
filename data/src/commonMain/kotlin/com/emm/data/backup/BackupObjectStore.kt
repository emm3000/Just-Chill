package com.emm.data.backup

/**
 * How many objects one [BackupObjectStore.list] call may return, and the number a caller must treat
 * as "this listing might not be everything".
 *
 * **The refusal it enables is the point, not the ceiling.** Supabase Storage's list takes a limit
 * with a server-side default and returns a page, not a bucket; a prune that runs against a partial
 * view can conclude a snapshot is the newest when it is not, and delete real snapshots to fill slots
 * that are already occupied by objects it cannot see. So the limit is set explicitly rather than
 * inherited, and a result of exactly this size aborts the prune — a skipped prune costs storage, a
 * prune on a truncated listing costs data.
 *
 * A thousand is roughly forty times what retention can ever leave behind (`7 + 8 + 12` snapshots,
 * each with a sidecar) and is the value Supabase's own bucket-migration example uses, so reaching it
 * means something is wrong rather than merely busy.
 */
internal const val BACKUP_LIST_LIMIT: Int = 1000

/**
 * The five things the snapshot pipeline does to remote object storage, and nothing else.
 *
 * **This seam is what makes the verification testable at all**, and that is its whole justification
 * rather than a hedge against swapping storage vendors. ADR 009 Phase 2's gate is "a test proving an
 * upload whose hash mismatches is NOT marked successful"; a mismatch cannot be provoked against a
 * real bucket, which by construction returns the bytes it was given, and the JVM host suite has no
 * network and no Supabase project. With the digest check on this side of the interface, a stub that
 * hands back different bytes is a two-line fixture (DIP, `docs/CODE_QUALITY.md`).
 *
 * It is deliberately not a "storage abstraction". There is no exists, no copy and no signed-URL
 * anything. [list] arrived exactly when this KDoc said it would — Phase 2c's retention prune needs
 * it and nothing before that did — and it arrived alone. Five operations because five are used.
 *
 * **Byte arrays and object keys, never files or streams.** The digest is taken over the exact
 * sequence that travels, so anything that could re-encode on the way through (a `String` payload, a
 * channel with a charset) would put a conversion between the hash and the wire — the failure
 * [sha256Hex]'s KDoc exists to rule out.
 */
internal interface BackupObjectStore {

    /**
     * The prefix every object of this snapshot must be stored under — uid plus a trailing `/` — or a
     * named failure if nobody is signed in.
     *
     * **The owning prefix comes from the session, never from a caller**, which is what makes the
     * bucket's access rules unbreakable from this side: every policy on `storage.objects` is keyed on
     * the first path segment equalling the caller's uid, so an implementation that let a caller
     * choose the prefix would be offering to write somewhere the server will refuse anyway. The
     * prefix and the SQL policy that constrains it stay one decision in one place
     * (`supabase/migrations/20260814200043_backup_storage_bucket.sql`).
     *
     * **It hands back a prefix rather than a finished key, and it is resolved once per snapshot.**
     * An earlier shape took a file name and returned the whole key, so a caller needing a payload and
     * a manifest called it twice — two independent reads of the session, and therefore two prefixes
     * whenever the session changed between them. The separator lives in the returned value so that
     * concatenation is all a caller does and nothing downstream gets to reinvent the layout.
     *
     * No session is a reported failure and never a silent no-op: ADR 009 Decision 1 says no session
     * means no pipeline, and a snapshot that quietly did not happen is the outage shape hard
     * constraint 4 forbids.
     */
    suspend fun ownedPrefix(): String

    /**
     * Writes [bytes] at [key], which must end in `.json`. Never an upsert — see the implementation
     * for why the server agrees, and for why the extension is a refusal rather than a convention.
     */
    suspend fun upload(key: String, bytes: ByteArray)

    /** Reads back exactly what is stored at [key]. */
    suspend fun download(key: String): ByteArray

    /** Removes [key]. Used to discard an object that failed verification, and to prune an old one. */
    suspend fun delete(key: String)

    /**
     * The objects stored **directly** under [prefix], as names relative to it, at most
     * [BACKUP_LIST_LIMIT] of them.
     *
     * Three properties the retention prune depends on, all of them the implementation's job to
     * guarantee rather than the caller's to work around:
     *
     * - **Relative names, so `prefix + name` is a key.** That is the same arithmetic the uploader
     *   does, which is what keeps one layout in one place.
     * - **Directly under, never recursive.** ADR 009 pins snapshots under a `pinned/` prefix the
     *   prune must never scan, and this is the operation that has to honour that. Supabase Storage
     *   treats `/` as a delimiter and answers with the nested folder as a single entry, so nothing
     *   inside it is ever returned; a listing that started returning full paths instead would be a
     *   contract break, not a formatting difference.
     * - **Objects only, no folder entries.** The same delimiter behaviour means a listing of
     *   `<uid>/` can contain `pinned` itself. A folder is not an object and cannot be deleted as
     *   one, so it never reaches the caller. (The prune survives one arriving anyway — a folder name
     *   parses as no snapshot and matches no sidecar — but defence in depth is not a reason to hand
     *   one over.)
     *
     * **A result of exactly [BACKUP_LIST_LIMIT] entries means the listing may be incomplete**, and
     * the caller must refuse to act on it. See that constant for why a partial listing is a
     * data-loss vector rather than an inconvenience.
     */
    suspend fun list(prefix: String): List<String>
}
