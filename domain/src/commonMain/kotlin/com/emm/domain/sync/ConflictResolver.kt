package com.emm.domain.sync

data class LocalRevision(val updatedAt: Long, val hasUnpushedEdit: Boolean)

class ConflictResolver {

    fun resolve(local: LocalRevision?, remoteUpdatedAt: Long): Resolution {
        if (local == null || !local.hasUnpushedEdit) return Resolution.ApplyRemote
        return if (local.updatedAt > remoteUpdatedAt) Resolution.KeepLocal else Resolution.ApplyRemote
    }
}

enum class Resolution { ApplyRemote, KeepLocal }
