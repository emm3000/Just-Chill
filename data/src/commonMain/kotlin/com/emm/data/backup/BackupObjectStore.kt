package com.emm.data.backup

/**
 * How many objects one [BackupObjectStore.list] call asks the server for.
 *
 * **It is a page size, not a ceiling on the bucket.** Supabase Storage's list takes a limit with a
 * server-side default and answers with a page; the caller pages through with an advancing offset
 * until a **short** page comes back, and a short page is what *proves* the listing is complete.
 * Nothing infers completeness from a count any more, which is the whole reason the number is here
 * rather than the refusal it used to enable: a page that came back full but held a folder entry
 * arrived at the caller one object short of the limit, so the refusal did not fire and the prune ran
 * on a truncated listing believing it was whole. ADR 009 mandates a `pinned/` folder, so that folder
 * entry is the designed state rather than an edge case.
 *
 * The limit is still set explicitly rather than inherited, because an unset one is whatever the
 * Storage API decides that day — a number a BOM bump can move without a compile error, and the
 * offset arithmetic below is built on knowing it.
 *
 * A thousand is roughly forty times what retention can ever leave behind (54 snapshots, each with a
 * sidecar) and is the value Supabase's own bucket-migration example uses, so a bucket that needs a
 * second page at all is already unusual.
 */
internal const val BACKUP_LIST_PAGE_SIZE: Int = 1000

/**
 * How many pages one listing may take before the caller gives up and refuses to prune.
 *
 * **The bound is there for the server, not for the bucket.** Retention leaves at most 54 snapshots
 * and their sidecars behind — 108 objects, a tenth of one page — so ten pages is about a hundred
 * times a healthy bucket and reaching it means something is wrong rather than merely busy. What it
 * actually rules out is a server that answers every page full: paging forever would hold ADR 009
 * Phase 2c's `launchOp` concurrent-op guard with no exception and no message, which is hard
 * constraint 4's exact shape. Hitting the cap deletes nothing, for the same reason a failed listing
 * deletes nothing — a partial view can make an old snapshot look like the newest.
 */
internal const val BACKUP_LIST_MAX_PAGES: Int = 10

/**
 * One page of a bucket listing: the object [names], and how many entries the server actually
 * returned.
 *
 * **The two counts are different and the difference is the whole point of this type.** [names] has
 * had folder entries dropped, so `names.size` can be short of a page that was in fact full. A pager
 * that compared the filtered size against its limit would read a full page as the last one and stop
 * early, and a listing truncated mid-way can cut between a payload and its sidecar — an orphan
 * payload is deleted on sight, so that is one verified snapshot gone, silently.
 *
 * [serverReturned] is the count before any filtering, and it is the only number a pager may compare
 * against the limit it asked for. It is carried here rather than left for the caller to work out
 * because the caller cannot: by the time a listing has crossed this seam, the folder entries that
 * made the page full are gone.
 */
internal data class ObjectPage(val names: List<String>, val serverReturned: Int)

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
     * One page of the objects stored **directly** under [prefix]: at most [limit] entries starting
     * at [offset], as names relative to the prefix, plus the raw count the server answered with.
     *
     * **It is one page and it says so**, which is what keeps a caller from mistaking a page for a
     * bucket. Completeness is proved by asking again at the next offset and getting a short page —
     * see [ObjectPage.serverReturned] for why the caller must compare *that* number against [limit]
     * and never the length of the names it received.
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
     *   one over.) It is still **counted** in [ObjectPage.serverReturned], because the server
     *   counted it.
     */
    suspend fun list(prefix: String, limit: Int, offset: Int): ObjectPage
}
