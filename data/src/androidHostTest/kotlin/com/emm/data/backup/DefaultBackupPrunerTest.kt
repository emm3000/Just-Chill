package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The half of the retention prune that knows there is a bucket: which objects are candidates, which
 * are leftovers, and what a failure is allowed to stop.
 *
 * The slot arithmetic is not tested here — `SnapshotRetentionTest` owns it, exhaustively, over ids
 * and timestamps with no storage in sight. What this file owns is the classification that decides
 * *what reaches* that arithmetic, and it is where ADR 009's rule lives or dies: **key on the sidecar,
 * never on a payload name alone**. A prune that filled its slots from bare names would hand one to
 * an unverified leftover and evict a verified snapshot to make room, and both objects are just names
 * in a listing, so nothing downstream would ever notice.
 *
 * The fake is hand-written for the same reason `DefaultBackupUploaderTest`'s is: [FakePrunableObjectStore.calls]
 * records the ORDER, and a real bucket cannot be asked to fail one delete and honour the next.
 */
class DefaultBackupPrunerTest {

    @Test
    fun `a complete pair inside retention is not touched`() = runTest {
        val store = storeOf(PAIR_PAYLOAD, PAIR_SIDECAR)

        val report = prunerOver(store).prune()

        assertEquals(listOf("list $PREFIX from 0"), store.calls)
        assertEquals(listOf(PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
        assertEquals(1, report.kept)
        assertEquals(0, report.deleted)
    }

    @Test
    fun `a payload with no sidecar is an orphan and goes on sight`() = runTest {
        val orphan = payload("2026-08-12")
        val store = storeOf(orphan, PAIR_PAYLOAD, PAIR_SIDECAR)

        val report = prunerOver(store).prune()

        assertEquals(listOf(PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
        assertEquals(1, report.deleted)
        assertEquals(emptyList(), report.failedDeletes)
    }

    @Test
    fun `a sidecar with no payload goes too, because it can only state a digest for bytes that are gone`() = runTest {
        val store = storeOf(manifestNameFor(payload("2026-08-12")), PAIR_PAYLOAD, PAIR_SIDECAR)

        prunerOver(store).prune()

        assertEquals(listOf(PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
    }

    @Test
    fun `a complete pair is never evicted in favour of an orphan`() = runTest {
        // The exact failure the manifest-keying exists to prevent, at its smallest. Both objects sit
        // on one calendar day, so they compete for one daily slot — and for the same weekly and
        // monthly slot. The orphan is the LATER of the two, so a prune that sorted bare names would
        // hand every slot to it and delete the verified pair as the loser.
        val orphan = payload("2026-08-14", hour = "18")
        val store = storeOf(orphan, PAIR_PAYLOAD, PAIR_SIDECAR)

        val report = prunerOver(store).prune()

        assertEquals(listOf(PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
        assertEquals(listOf("list $PREFIX from 0", "delete $PREFIX$orphan"), store.calls)
        assertEquals(1, report.kept)
    }

    @Test
    fun `nothing under the pinned prefix is a candidate or a delete target`() = runTest {
        // A real listing is not recursive, so only the folder entry `pinned` could ever show up —
        // and the store drops even that. The nested names are here to prove the second line of
        // defence holds on its own: ADR 009 promises a pinned snapshot survives a schema migration,
        // and that promise must not rest on one filter in one class.
        val pinnedPayload = "pinned/${payload("2026-01-02")}"
        val untouchable = listOf("pinned", pinnedPayload, manifestNameFor(pinnedPayload))
        val store = storeOf(*untouchable.toTypedArray(), PAIR_PAYLOAD, PAIR_SIDECAR)

        prunerOver(store).prune()

        assertEquals(untouchable + listOf(PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
    }

    @Test
    fun `a name this build cannot parse is left alone, not deleted`() = runTest {
        // None of these is a snapshot, so none is a retention candidate — and a prune that deleted
        // what it did not recognise would be the worst possible reading of "tidy up". The v4 name
        // matters most: the bucket outlives any single build.
        val strangers = listOf("notes.txt", "justchill-backup-2026-08-14.json", "backup-v4-2026-08-14T12-00-00Z.json")
        val store = storeOf(*strangers.toTypedArray(), PAIR_PAYLOAD, PAIR_SIDECAR)

        prunerOver(store).prune()

        assertEquals(strangers + listOf(PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
    }

    @Test
    fun `a listing that could not be read aborts before any delete`() = runTest {
        val store = storeOf(payload("2026-08-12"), PAIR_PAYLOAD, PAIR_SIDECAR)
        store.failList = IllegalStateException("the socket died")

        val failure = assertFailsWith<DomainException.Unknown> { prunerOver(store).prune() }

        assertEquals(
            "Snapshot retention prune failed: the bucket could not be listed, so nothing was deleted.",
            failure.message,
        )
        assertEquals(listOf("list $PREFIX from 0"), store.calls)
        assertEquals(3, store.objects.size)
    }

    @Test
    fun `a full page is followed by another, and the pair split across the boundary survives`() = runTest {
        // The boundary is where truncation costs data: the payload is the last entry of page one and
        // its sidecar the first of page two, so a prune that stopped at one page would see an orphan
        // and delete a verified snapshot on sight. Assembling both pages first is what makes the pair
        // a pair.
        val filler = List(BACKUP_LIST_PAGE_SIZE - 1) { manifestNameFor(payload("2026-01-02", minuteSecond = it)) }
        val store = storeOf(*(filler + listOf(PAIR_PAYLOAD, PAIR_SIDECAR)).toTypedArray())

        val report = prunerOver(store).prune()

        assertEquals(listOf("list $PREFIX from 0", "list $PREFIX from $BACKUP_LIST_PAGE_SIZE"), store.calls.take(2))
        assertEquals(1, report.kept)
        // Every filler entry is a sidecar whose payload does not exist, so all of them go and the
        // pair stays: the second page was read, and it was read as part of one listing.
        assertEquals(filler.size, report.deleted)
        assertEquals(listOf(PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
    }

    @Test
    fun `a page that is full only because of a folder entry is still a full page`() = runTest {
        // The regression, named: the store drops folder entries, so this page arrives one object
        // short of the limit while the server reported it full. A pager that counted the names it
        // received would call it the last page and never read the sidecar sitting on page two —
        // turning the verified pair into an orphan the prune deletes on sight.
        val filler = List(BACKUP_LIST_PAGE_SIZE - 1) { manifestNameFor(payload("2026-01-02", minuteSecond = it)) }
        val store = storeOf(*(filler + listOf(PAIR_PAYLOAD, PAIR_SIDECAR)).toTypedArray())
        store.foldersOnFirstPage = 1

        val report = prunerOver(store).prune()

        assertEquals(listOf("list $PREFIX from 0", "list $PREFIX from $BACKUP_LIST_PAGE_SIZE"), store.calls.take(2))
        assertEquals(1, report.kept)
        assertEquals(listOf(PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
    }

    @Test
    fun `a server that never stops paging is refused, and deletes nothing`() = runTest {
        val store = storeOf(payload("2026-08-12"), PAIR_PAYLOAD, PAIR_SIDECAR)
        store.neverEnds = true

        val failure = assertFailsWith<DomainException.Unknown> { prunerOver(store).prune() }

        assertTrue(failure.message.orEmpty().startsWith("Snapshot retention prune failed:"), failure.message.orEmpty())
        assertTrue(
            failure.message.orEmpty().contains("$BACKUP_LIST_MAX_PAGES pages of $BACKUP_LIST_PAGE_SIZE objects"),
            failure.message.orEmpty(),
        )
        assertEquals(BACKUP_LIST_MAX_PAGES, store.calls.size)
        assertEquals(3, store.objects.size)
    }

    @Test
    fun `a failure on the second page aborts the whole prune, not just that page`() = runTest {
        // Half a listing is a partial view of the bucket, and a partial view can make an old snapshot
        // look like the newest. Same answer as a first page that failed: nothing is deleted.
        val filler = List(BACKUP_LIST_PAGE_SIZE) { manifestNameFor(payload("2026-01-02", minuteSecond = it)) }
        val store = storeOf(*(filler + listOf(PAIR_PAYLOAD, PAIR_SIDECAR)).toTypedArray())
        store.failListAtOffset = BACKUP_LIST_PAGE_SIZE

        val failure = assertFailsWith<DomainException.Unknown> { prunerOver(store).prune() }

        assertEquals(
            "Snapshot retention prune failed: the bucket could not be listed, so nothing was deleted.",
            failure.message,
        )
        assertEquals(listOf("list $PREFIX from 0", "list $PREFIX from $BACKUP_LIST_PAGE_SIZE"), store.calls)
        assertEquals(filler.size + 2, store.objects.size)
    }

    @Test
    fun `a delete that fails takes neither the run nor the remaining deletes with it`() = runTest {
        val first = payload("2026-08-10")
        val stuck = payload("2026-08-11")
        val last = payload("2026-08-12")
        val store = storeOf(first, stuck, last, PAIR_PAYLOAD, PAIR_SIDECAR)
        store.failDeleteOf += "$PREFIX$stuck"

        val report = prunerOver(store).prune()

        // No throw: one object nobody could remove is a leftover the next run sees again, not a
        // reason to leave the rest of the bucket uncleaned.
        assertEquals(listOf(stuck, PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
        assertEquals(2, report.deleted)
        // But it is not silent either — hard constraint 4. This is the ONLY channel it has, since a
        // delete failure cannot be allowed to throw.
        assertEquals(1, report.failedDeletes.size)
        assertTrue(report.failedDeletes.single().startsWith(stuck), report.failedDeletes.single())
    }

    @Test
    fun `an evicted pair loses its sidecar before its payload`() = runTest {
        // Two pairs on one calendar day compete for one slot and the later one takes it. If the
        // second delete failed, what survives must be a payload with no receipt — the leftover the
        // rules already handle — rather than a receipt for bytes that are gone, which is the one
        // shape a pre-restore check cannot catch. Same order, same reason, as the uploader's cleanup.
        val older = payload("2026-08-14", hour = "06")
        val store = storeOf(older, manifestNameFor(older), PAIR_PAYLOAD, PAIR_SIDECAR)

        val report = prunerOver(store).prune()

        assertEquals(
            listOf("list $PREFIX from 0", "delete $PREFIX${manifestNameFor(older)}", "delete $PREFIX$older"),
            store.calls,
        )
        assertEquals(listOf(PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
        assertEquals(1, report.kept)
        assertEquals(2, report.deleted)
    }

    @Test
    fun `the owning prefix is resolved once, not once per object`() = runTest {
        val store = storeOf(payload("2026-08-10"), payload("2026-08-11"), PAIR_PAYLOAD, PAIR_SIDECAR)

        prunerOver(store).prune()

        // Two resolutions would be two reads of the session, so a sign-out landing between them
        // would have the second half of this run addressing keys under somebody else's prefix.
        assertEquals(1, store.prefixResolutions)
    }

    @Test
    fun `no session stops the prune before anything is listed`() = runTest {
        val store = storeOf(payload("2026-08-10"))
        store.failOwnedPrefix = DomainException.Unauthorized("nobody is signed in")

        val failure = assertFailsWith<DomainException.Unauthorized> { prunerOver(store).prune() }

        // Let through untouched rather than rewrapped as "the owning prefix could not be resolved":
        // the real text and type belong to SupabaseBackupObjectStore, and this is not a transport
        // problem being reported as one.
        assertEquals("nobody is signed in", failure.message)
        assertEquals(emptyList(), store.calls)
    }
}

private fun prunerOver(store: FakePrunableObjectStore): DefaultBackupPruner =
    DefaultBackupPruner(store, FixedClock(NOW), LIMA)

private fun storeOf(vararg names: String): FakePrunableObjectStore = FakePrunableObjectStore(names.toList())

/**
 * A snapshot payload name for [day], stamped in UTC exactly the way the builder does it.
 *
 * [minuteSecond] exists only for the truncated-listing fixture, which needs a thousand distinct
 * names and has only twenty-four hours to spend.
 */
private fun payload(day: String, hour: String = "12", minuteSecond: Int = 0): String {
    val minutes = (minuteSecond / SECONDS_PER_MINUTE).toString().padStart(2, '0')
    val seconds = (minuteSecond % SECONDS_PER_MINUTE).toString().padStart(2, '0')
    return "backup-v$BACKUP_SCHEMA_VERSION-${day}T$hour-$minutes-${seconds}Z.json"
}

/**
 * An in-memory bucket listing that can be told to misbehave in the ways a real one cannot.
 *
 * [objects] holds names RELATIVE to the prefix, which is the shape [BackupObjectStore.list] promises;
 * [delete] takes a full key, so the round trip through `prefix + name` is exercised rather than
 * assumed. [upload] and [download] throw: the prune has no business calling either, and a fixture
 * that quietly tolerated one would hide it.
 *
 * It pages the way the server does — `limit` entries from `offset`, in the order it was handed them —
 * and it can report a page as fuller than the names it hands over ([foldersOnFirstPage]), which is
 * the one behaviour a real bucket has and a naive fake does not.
 */
private class FakePrunableObjectStore(names: List<String>) : BackupObjectStore {

    val objects: MutableList<String> = names.toMutableList()
    val calls: MutableList<String> = mutableListOf()

    var prefixResolutions: Int = 0
        private set

    var failOwnedPrefix: Throwable? = null
    var failList: Throwable? = null

    /** Fails the page that starts at this offset, so a mid-pagination failure is expressible. */
    var failListAtOffset: Int? = null

    /**
     * Folder entries the server counts and never hands over. ADR 009 mandates a `pinned/` folder, so
     * a first page whose raw size only reaches the limit because of one is the designed state.
     */
    var foldersOnFirstPage: Int = 0

    /** Answers every page full, forever — the misbehaving server the page cap exists for. */
    var neverEnds: Boolean = false

    /** Full keys whose delete fails, so one stuck object and a healthy bucket are expressible together. */
    val failDeleteOf: MutableSet<String> = mutableSetOf()

    override suspend fun ownedPrefix(): String {
        prefixResolutions++
        failOwnedPrefix?.let { throw it }
        return PREFIX
    }

    override suspend fun list(prefix: String, limit: Int, offset: Int): ObjectPage {
        calls += "list $prefix from $offset"
        failList?.let { throw it }
        if (offset == failListAtOffset) throw IllegalStateException("the socket died mid-listing")
        if (neverEnds) return ObjectPage(names = emptyList(), serverReturned = limit)

        // The server counts folder entries in both the page size and the offset, so an object's
        // index trails the offset by however many folders came before it.
        val folders = if (offset == 0) foldersOnFirstPage else 0
        val firstObject = (offset - foldersOnFirstPage).coerceAtLeast(0)
        val page: List<String> = objects.drop(firstObject).take(limit - folders)
        return ObjectPage(names = page, serverReturned = page.size + folders)
    }

    override suspend fun delete(key: String) {
        calls += "delete $key"
        if (key in failDeleteOf) throw IllegalStateException("boom")
        objects -= key.removePrefix(PREFIX)
    }

    override suspend fun upload(key: String, bytes: ByteArray): Unit = error("the prune never uploads")

    override suspend fun download(key: String): ByteArray = error("the prune never downloads")
}

/** Reads the same instant every time, so a bucket boundary can only move when a test moves it. */
private class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

private const val UID = "5f1a2b3c-0000-4000-8000-000000000001"

private const val PREFIX = "$UID/"

private const val SECONDS_PER_MINUTE = 60

private val LIMA: TimeZone = TimeZone.of("America/Lima")

private val NOW: Instant = Instant.parse("2026-08-15T00:00:00Z")

/** The one verified pair every fixture keeps around, so "kept" is never vacuously true. */
private val PAIR_PAYLOAD: String = payload("2026-08-14")

private val PAIR_SIDECAR: String = manifestNameFor(PAIR_PAYLOAD)
