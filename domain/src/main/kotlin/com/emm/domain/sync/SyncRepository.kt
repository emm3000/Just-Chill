package com.emm.domain.sync

import com.emm.domain.shared.error.DomainException

/**
 * Synchronises local data with the remote backend: push pending rows, then pull remote
 * changes using the server-set cursor (ADR 002).
 *
 * @throws DomainException on failure.
 */
interface SyncRepository {
    suspend fun sync()
}
