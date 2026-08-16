package com.emm.data.backup

import com.emm.domain.shared.error.DomainException

internal const val BACKUP_LIST_PAGE_SIZE: Int = 1000

internal const val BACKUP_LIST_MAX_PAGES: Int = 10

internal data class ObjectPage(val names: List<String>, val serverReturned: Int)

internal fun ownerPrefixOf(userId: String): String = "$userId/"

internal interface BackupObjectStore {

    suspend fun ownedPrefix(): String

    suspend fun upload(key: String, bytes: ByteArray)

    suspend fun download(key: String): ByteArray

    suspend fun delete(key: String)

    suspend fun list(prefix: String, limit: Int, offset: Int): ObjectPage
}

// Every caller needs the same bound and the same refusal: a listing that never ends is a partial
// view of the bucket, and both readers of it (prune, verification) draw a conclusion that is only
// valid over the whole thing. The reasons differ per caller, the bound does not.
internal suspend fun BackupObjectStore.wholeBucket(
    prefix: String,
    listFailed: String,
    listingNeverEnded: String,
): List<String> {
    val names = mutableListOf<String>()
    var offset = 0
    repeat(BACKUP_LIST_MAX_PAGES) {
        val page: ObjectPage = storageCall(listFailed) { list(prefix, BACKUP_LIST_PAGE_SIZE, offset) }
        names += page.names
        if (page.serverReturned == 0) return names
        offset += page.serverReturned
    }
    throw DomainException.Unknown(IllegalStateException(listingNeverEnded), listingNeverEnded)
}
