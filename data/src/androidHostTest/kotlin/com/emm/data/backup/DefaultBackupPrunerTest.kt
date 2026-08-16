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

class DefaultBackupPrunerTest {

    @Test
    fun `a complete pair inside retention is not touched`() = runTest {
        val store = storeOf(PAIR_PAYLOAD, PAIR_SIDECAR)

        val report = prunerOver(store).prune()

        assertEquals(listOf("list $PREFIX from 0", "list $PREFIX from 2"), store.calls)
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
        val orphan = payload("2026-08-14", hour = "18")
        val store = storeOf(orphan, PAIR_PAYLOAD, PAIR_SIDECAR)

        val report = prunerOver(store).prune()

        assertEquals(listOf(PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
        assertEquals(
            listOf("list $PREFIX from 0", "list $PREFIX from 3", "delete $PREFIX$orphan"),
            store.calls,
        )
        assertEquals(1, report.kept)
    }

    @Test
    fun `nothing under the pinned prefix is a candidate or a delete target`() = runTest {
        val pinnedPayload = "pinned/${payload("2026-01-02")}"
        val untouchable = listOf("pinned", pinnedPayload, manifestNameFor(pinnedPayload))
        val store = storeOf(*untouchable.toTypedArray(), PAIR_PAYLOAD, PAIR_SIDECAR)

        prunerOver(store).prune()

        assertEquals(untouchable + listOf(PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
    }

    @Test
    fun `a name this build cannot parse is left alone, not deleted`() = runTest {
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
        val filler = List(BACKUP_LIST_PAGE_SIZE - 1) { manifestNameFor(payload("2026-01-02", minuteSecond = it)) }
        val store = storeOf(*(filler + listOf(PAIR_PAYLOAD, PAIR_SIDECAR)).toTypedArray())

        val report = prunerOver(store).prune()

        assertEquals(listOf("list $PREFIX from 0", "list $PREFIX from $BACKUP_LIST_PAGE_SIZE"), store.calls.take(2))
        assertEquals(1, report.kept)
        assertEquals(filler.size, report.deleted)
        assertEquals(listOf(PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
    }

    @Test
    fun `a page that is full only because of a folder entry is still a full page`() = runTest {
        val filler = List(BACKUP_LIST_PAGE_SIZE - 1) { manifestNameFor(payload("2026-01-02", minuteSecond = it)) }
        val store = storeOf(*(filler + listOf(PAIR_PAYLOAD, PAIR_SIDECAR)).toTypedArray())
        store.foldersOnFirstPage = 1

        val report = prunerOver(store).prune()

        assertEquals(listOf("list $PREFIX from 0", "list $PREFIX from $BACKUP_LIST_PAGE_SIZE"), store.calls.take(2))
        assertEquals(1, report.kept)
        assertEquals(listOf(PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
    }

    @Test
    fun `a server that clamps every page below the requested size still assembles the complete listing`() = runTest {
        val strangers = List(9) { "stranger-$it.txt" }
        val store = storeOf(*(strangers + listOf(PAIR_PAYLOAD, PAIR_SIDECAR)).toTypedArray())
        store.clampPageSizeTo = CLAMP_LIMIT

        val report = prunerOver(store).prune()

        assertEquals(
            listOf("list $PREFIX from 0", "list $PREFIX from $CLAMP_LIMIT", "list $PREFIX from ${CLAMP_LIMIT + 1}"),
            store.calls,
        )
        assertEquals(1, report.kept)
        assertEquals(0, report.deleted)
        assertEquals(strangers + listOf(PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
    }

    @Test
    fun `a page reporting zero objects ends the loop even though more objects remain unread`() = runTest {
        val store = storeOf(PAIR_PAYLOAD, PAIR_SIDECAR, payload("2026-08-10"))
        store.clampPageSizeTo = 1
        store.zeroPageAtOffset = 1

        val report = prunerOver(store).prune()

        assertEquals(
            listOf("list $PREFIX from 0", "list $PREFIX from 1", "delete $PREFIX$PAIR_PAYLOAD"),
            store.calls,
        )
        assertEquals(0, report.kept)
        assertEquals(1, report.deleted)
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

        assertEquals(listOf(stuck, PAIR_PAYLOAD, PAIR_SIDECAR), store.objects)
        assertEquals(2, report.deleted)
        assertEquals(1, report.failedDeletes.size)
        assertTrue(report.failedDeletes.single().startsWith(stuck), report.failedDeletes.single())
    }

    @Test
    fun `an evicted pair loses its sidecar before its payload`() = runTest {
        val older = payload("2026-08-14", hour = "06")
        val store = storeOf(older, manifestNameFor(older), PAIR_PAYLOAD, PAIR_SIDECAR)

        val report = prunerOver(store).prune()

        assertEquals(
            listOf(
                "list $PREFIX from 0",
                "list $PREFIX from 4",
                "delete $PREFIX${manifestNameFor(older)}",
                "delete $PREFIX$older",
            ),
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

        assertEquals(1, store.prefixResolutions)
    }

    @Test
    fun `no session stops the prune before anything is listed`() = runTest {
        val store = storeOf(payload("2026-08-10"))
        store.failOwnedPrefix = DomainException.Unauthorized("nobody is signed in")

        val failure = assertFailsWith<DomainException.Unauthorized> { prunerOver(store).prune() }

        assertEquals("nobody is signed in", failure.message)
        assertEquals(emptyList(), store.calls)
    }
}

private fun prunerOver(store: FakePrunableObjectStore): DefaultBackupPruner =
    DefaultBackupPruner(store, FixedClock(NOW), LIMA)

private fun storeOf(vararg names: String): FakePrunableObjectStore = FakePrunableObjectStore(names.toList())

private fun payload(day: String, hour: String = "12", minuteSecond: Int = 0): String {
    val minutes = (minuteSecond / SECONDS_PER_MINUTE).toString().padStart(2, '0')
    val seconds = (minuteSecond % SECONDS_PER_MINUTE).toString().padStart(2, '0')
    return "backup-v$BACKUP_SCHEMA_VERSION-${day}T$hour-$minutes-${seconds}Z.json"
}

private class FakePrunableObjectStore(names: List<String>) : BackupObjectStore {

    val objects: MutableList<String> = names.toMutableList()
    val calls: MutableList<String> = mutableListOf()

    var prefixResolutions: Int = 0
        private set

    var failOwnedPrefix: Throwable? = null
    var failList: Throwable? = null

    var failListAtOffset: Int? = null

    var foldersOnFirstPage: Int = 0

    var neverEnds: Boolean = false

    var clampPageSizeTo: Int? = null

    var zeroPageAtOffset: Int? = null

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

        val folders = if (offset == 0) foldersOnFirstPage else 0
        val firstObject = (offset - foldersOnFirstPage).coerceAtLeast(0)
        val effectiveLimit = clampPageSizeTo?.coerceAtMost(limit) ?: limit

        return when {
            neverEnds -> ObjectPage(names = emptyList(), serverReturned = limit)

            offset == zeroPageAtOffset -> ObjectPage(names = emptyList(), serverReturned = 0)

            else -> {
                val page: List<String> = objects.drop(firstObject).take(effectiveLimit - folders)
                ObjectPage(names = page, serverReturned = page.size + folders)
            }
        }
    }

    override suspend fun delete(key: String) {
        calls += "delete $key"
        if (key in failDeleteOf) throw IllegalStateException("boom")
        objects -= key.removePrefix(PREFIX)
    }

    override suspend fun upload(key: String, bytes: ByteArray): Unit = error("the prune never uploads")

    override suspend fun download(key: String): ByteArray = error("the prune never downloads")
}

private class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

private const val UID = "5f1a2b3c-0000-4000-8000-000000000001"

private const val PREFIX = "$UID/"

private const val SECONDS_PER_MINUTE = 60

private const val CLAMP_LIMIT = 10

private val LIMA: TimeZone = TimeZone.of("America/Lima")

private val NOW: Instant = Instant.parse("2026-08-15T00:00:00Z")

private val PAIR_PAYLOAD: String = payload("2026-08-14")

private val PAIR_SIDECAR: String = manifestNameFor(PAIR_PAYLOAD)
