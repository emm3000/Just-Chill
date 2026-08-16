package com.emm.data.backup

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
