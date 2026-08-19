package com.emm.data.backup

import com.emm.domain.shared.error.DomainException

internal const val BACKUP_LIST_PAGE_SIZE: Int = 1000

internal const val BACKUP_LIST_MAX_PAGES: Int = 10

/**
 * [serverReturned] counts the raw rows the server sent, folders included; [names] holds only the
 * rows that are objects. A caller proving a prefix is empty has to read [serverReturned].
 */
internal data class ObjectPage(
    val names: List<String>,
    val serverReturned: Int,
    val folders: List<String> = emptyList(),
)

internal data class BucketListing(val names: List<String>, val folders: List<String>)

internal fun ownerPrefixOf(userId: String): String = "$userId/"

internal interface BackupObjectStore {

    suspend fun ownedPrefix(): String

    suspend fun upload(key: String, bytes: ByteArray)

    suspend fun download(key: String): ByteArray

    suspend fun delete(key: String)

    suspend fun list(prefix: String, limit: Int, offset: Int): ObjectPage
}

internal suspend fun BackupObjectStore.wholeBucket(
    prefix: String,
    listFailed: String,
    listingNeverEnded: String,
): BucketListing {
    val names = mutableListOf<String>()
    val folders = mutableListOf<String>()
    var offset = 0
    repeat(BACKUP_LIST_MAX_PAGES) {
        val page: ObjectPage = storageCall(listFailed) { list(prefix, BACKUP_LIST_PAGE_SIZE, offset) }
        names += page.names
        folders += page.folders
        if (page.serverReturned == 0) return BucketListing(names, folders)
        offset += page.serverReturned
    }
    throw DomainException.Unknown(IllegalStateException(listingNeverEnded), listingNeverEnded)
}
