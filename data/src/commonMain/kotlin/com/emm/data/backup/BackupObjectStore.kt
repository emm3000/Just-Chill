package com.emm.data.backup

/**
 * The four things the snapshot pipeline does to remote object storage, and nothing else.
 *
 * **This seam is what makes the verification testable at all**, and that is its whole justification
 * rather than a hedge against swapping storage vendors. ADR 009 Phase 2's gate is "a test proving an
 * upload whose hash mismatches is NOT marked successful"; a mismatch cannot be provoked against a
 * real bucket, which by construction returns the bytes it was given, and the JVM host suite has no
 * network and no Supabase project. With the digest check on this side of the interface, a stub that
 * hands back different bytes is a two-line fixture (DIP, `docs/CODE_QUALITY.md`).
 *
 * It is deliberately not a "storage abstraction". There is no list, no exists, no copy and no
 * signed-URL anything — Phase 2c's retention prune will need list and it can add it then. Four
 * operations because four are used.
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

    /** Removes [key]. Used only to discard an object that failed verification. */
    suspend fun delete(key: String)
}
