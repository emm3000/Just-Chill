package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultBackupEraserTest {

    @Test
    fun `every folder the listing discovers is walked and everything under the prefix is deleted`() = runTest {
        val store = storeOf(PAYLOAD_KEY, SIDECAR_KEY, PINNED_KEY)

        eraserOver(store).eraseOwnedBackups(UID)

        assertEquals(
            listOf(
                "list $PREFIX from 0",
                "list $PREFIX from 3",
                "list $PINNED_PREFIX from 0",
                "list $PINNED_PREFIX from 1",
                "delete $PAYLOAD_KEY",
                "delete $SIDECAR_KEY",
                "delete $PINNED_KEY",
                "list $PREFIX from 0",
            ),
            store.calls,
        )
        assertEquals(emptyList(), store.remaining())
    }

    @Test
    fun `an object under a folder that is not pinned is found and deleted too`() = runTest {
        val store = storeOf(EXPORTS_KEY)

        eraserOver(store).eraseOwnedBackups(UID)

        assertTrue(store.calls.contains("list ${PREFIX}exports/ from 0"), store.calls.toString())
        assertEquals(listOf("delete $EXPORTS_KEY"), store.calls.filter { it.startsWith("delete") })
        assertEquals(emptyList(), store.remaining())
    }

    @Test
    fun `a failed listing of the bare prefix aborts before the first delete`() = runTest {
        val store = storeOf(PAYLOAD_KEY, SIDECAR_KEY, PINNED_KEY)
        store.failListOf += PREFIX

        val failure = assertFailsWith<DomainException.BackupsNotErased> { eraserOver(store).eraseOwnedBackups(UID) }

        assertEquals(
            "Account backup erase failed: the bucket could not be listed, so nothing was deleted.",
            failure.message,
        )
        assertEquals(listOf("list $PREFIX from 0"), store.calls)
        assertEquals(listOf(PAYLOAD_KEY, SIDECAR_KEY, PINNED_KEY), store.remaining())
    }

    @Test
    fun `a failed listing of a discovered folder aborts before the first delete too`() = runTest {
        val store = storeOf(PAYLOAD_KEY, SIDECAR_KEY, PINNED_KEY)
        store.failListOf += PINNED_PREFIX

        val failure = assertFailsWith<DomainException.BackupsNotErased> { eraserOver(store).eraseOwnedBackups(UID) }

        assertEquals(
            "Account backup erase failed: the bucket could not be listed, so nothing was deleted.",
            failure.message,
        )
        assertEquals(emptyList(), store.calls.filter { it.startsWith("delete") })
        assertEquals(listOf(PAYLOAD_KEY, SIDECAR_KEY, PINNED_KEY), store.remaining())
    }

    @Test
    fun `a server that never stops paging is refused, and deletes nothing`() = runTest {
        val store = storeOf(PAYLOAD_KEY, SIDECAR_KEY)
        store.neverEnds = true

        val failure = assertFailsWith<DomainException.BackupsNotErased> { eraserOver(store).eraseOwnedBackups(UID) }

        assertTrue(
            failure.message.orEmpty().contains("$BACKUP_LIST_MAX_PAGES pages of $BACKUP_LIST_PAGE_SIZE objects"),
            failure.message.orEmpty(),
        )
        assertEquals(BACKUP_LIST_MAX_PAGES, store.calls.size)
        assertEquals(listOf(PAYLOAD_KEY, SIDECAR_KEY), store.remaining())
    }

    @Test
    fun `a tree nested deeper than the walk allows is refused, and deletes nothing`() = runTest {
        val store = storeOf("${PREFIX}a/b/c/d/e/f/$PAYLOAD")

        val failure = assertFailsWith<DomainException.BackupsNotErased> { eraserOver(store).eraseOwnedBackups(UID) }

        assertTrue(failure.message.orEmpty().contains("nests deeper than"), failure.message.orEmpty())
        assertEquals(emptyList(), store.calls.filter { it.startsWith("delete") })
        assertEquals(listOf("${PREFIX}a/b/c/d/e/f/$PAYLOAD"), store.remaining())
    }

    @Test
    fun `a delete that fails takes neither the run nor the remaining deletes with it`() = runTest {
        val store = storeOf(PAYLOAD_KEY, SIDECAR_KEY, PINNED_KEY)
        store.failDeleteOf += PAYLOAD_KEY

        val failure = assertFailsWith<DomainException.BackupsNotErased> { eraserOver(store).eraseOwnedBackups(UID) }

        assertEquals(1, failure.failedCount)
        assertEquals(listOf(PAYLOAD_KEY), store.remaining())
    }

    @Test
    fun `every survivor is counted, at any depth`() = runTest {
        val store = storeOf(PAYLOAD_KEY, SIDECAR_KEY, PINNED_KEY)
        store.failDeleteOf += SIDECAR_KEY
        store.failDeleteOf += PINNED_KEY

        val failure = assertFailsWith<DomainException.BackupsNotErased> { eraserOver(store).eraseOwnedBackups(UID) }

        assertEquals(2, failure.failedCount)
        assertEquals(listOf(SIDECAR_KEY, PINNED_KEY), store.remaining())
    }

    @Test
    fun `an object still listed after every delete succeeded refuses the erase`() = runTest {
        val store = storeOf(PAYLOAD_KEY, SIDECAR_KEY)
        store.survivesDeleteSilently += SIDECAR_KEY

        val failure = assertFailsWith<DomainException.BackupsNotErased> { eraserOver(store).eraseOwnedBackups(UID) }

        assertEquals(1, failure.failedCount)
        assertTrue(failure.message.orEmpty().contains("still listed"), failure.message.orEmpty())
        assertEquals(
            listOf("delete $PAYLOAD_KEY", "delete $SIDECAR_KEY"),
            store.calls.filter { it.startsWith("delete") },
        )
    }

    @Test
    fun `an empty prefix is a successful sweep, not a failure`() = runTest {
        val store = storeOf()

        eraserOver(store).eraseOwnedBackups(UID)

        assertEquals(emptyList(), store.calls.filter { it.startsWith("delete") })
    }

    @Test
    fun `no session stops the sweep before anything is listed`() = runTest {
        val store = storeOf(PAYLOAD_KEY)
        store.failOwnedPrefix = DomainException.Unauthorized("nobody is signed in")

        val failure = assertFailsWith<DomainException.BackupsNotErased> { eraserOver(store).eraseOwnedBackups(UID) }

        assertEquals("nobody is signed in", failure.message)
        assertEquals(emptyList(), store.calls)
    }

    @Test
    fun `the owning prefix is resolved once, not once per listing`() = runTest {
        val store = storeOf(PAYLOAD_KEY, PINNED_KEY)

        eraserOver(store).eraseOwnedBackups(UID)

        assertEquals(1, store.prefixResolutions)
    }

    @Test
    fun `a live session owning another prefix stops the sweep before anything is listed`() = runTest {
        val store = storeOf(PAYLOAD_KEY, PINNED_KEY)
        store.ownedPrefixValue = "$OTHER_UID/"

        val failure = assertFailsWith<DomainException.BackupsNotErased> { eraserOver(store).eraseOwnedBackups(UID) }

        assertTrue(failure.message.orEmpty().contains("not the one being deleted"), failure.message.orEmpty())
        assertEquals(emptyList(), store.calls)
        assertEquals(listOf(PAYLOAD_KEY, PINNED_KEY), store.remaining())
    }
}

private fun eraserOver(store: FakeErasableObjectStore): DefaultBackupEraser = DefaultBackupEraser(store)

private fun storeOf(vararg keys: String): FakeErasableObjectStore = FakeErasableObjectStore(keys.toList())

/**
 * Models what `SupabaseBackupObjectStore.list` returns for one prefix: the objects directly under it
 * as prefix-relative names, plus one folder pseudo-row per distinct first segment below it. Both
 * kinds count towards `serverReturned`, and a folder is only reachable by listing it in turn.
 */
private class FakeErasableObjectStore(keys: List<String>) : BackupObjectStore {

    private val objects: MutableList<String> = keys.toMutableList()

    val calls: MutableList<String> = mutableListOf()

    var prefixResolutions: Int = 0
        private set

    var failOwnedPrefix: Throwable? = null

    var ownedPrefixValue: String = PREFIX

    var neverEnds: Boolean = false

    val failListOf: MutableSet<String> = mutableSetOf()

    val failDeleteOf: MutableSet<String> = mutableSetOf()

    val survivesDeleteSilently: MutableSet<String> = mutableSetOf()

    fun remaining(): List<String> = objects.toList()

    override suspend fun ownedPrefix(): String {
        prefixResolutions++
        failOwnedPrefix?.let { throw it }
        return ownedPrefixValue
    }

    override suspend fun list(prefix: String, limit: Int, offset: Int): ObjectPage {
        calls += "list $prefix from $offset"
        if (prefix in failListOf) throw IllegalStateException("the socket died")
        if (neverEnds) return ObjectPage(names = emptyList(), serverReturned = limit)

        val page: List<String> = rowsUnder(prefix).drop(offset).take(limit)
        return ObjectPage(
            names = page.filterNot { it.endsWith('/') },
            serverReturned = page.size,
            folders = page.filter { it.endsWith('/') }.map { it.removeSuffix("/") },
        )
    }

    override suspend fun delete(key: String) {
        calls += "delete $key"
        if (key in failDeleteOf) throw IllegalStateException("boom")
        if (key in survivesDeleteSilently) return
        objects -= key
    }

    override suspend fun upload(key: String, bytes: ByteArray): Unit = error("the sweep never uploads")

    override suspend fun download(key: String): ByteArray = error("the sweep never downloads")

    private fun rowsUnder(prefix: String): List<String> {
        val relative: List<String> = objects.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }
        val folders: List<String> = relative.filter { '/' in it }.map { it.substringBefore('/') + "/" }
        return (folders + relative.filterNot { '/' in it }).distinct().sorted()
    }
}

private const val UID = "5f1a2b3c-0000-4000-8000-000000000001"

private const val OTHER_UID = "5f1a2b3c-0000-4000-8000-000000000002"

private const val PREFIX = "$UID/"

private const val PINNED_PREFIX = "${PREFIX}pinned/"

private const val PAYLOAD = "backup-v3-2026-08-14T12-00-00Z.json"

private const val PAYLOAD_KEY = "$PREFIX$PAYLOAD"

private const val SIDECAR_KEY = "$PAYLOAD_KEY.manifest.json"

private const val PINNED_KEY = "${PINNED_PREFIX}backup-v3-2026-01-02T12-00-00Z.json"

private const val EXPORTS_KEY = "${PREFIX}exports/$PAYLOAD"
