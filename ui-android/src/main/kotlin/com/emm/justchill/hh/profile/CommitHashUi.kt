package com.emm.justchill.hh.profile

const val UNKNOWN_COMMIT_HASH: String = "unknown"

private const val SHORT_COMMIT_HASH_LENGTH = 7

sealed interface CommitHashUi {

    val label: String

    data class Available(val fullHash: String) : CommitHashUi {
        override val label: String = "Commit ${fullHash.take(SHORT_COMMIT_HASH_LENGTH)}"
    }

    data object Unavailable : CommitHashUi {
        override val label: String = "Commit no disponible"
    }
}

fun commitHashUi(commitHash: String): CommitHashUi = when {
    commitHash.isBlank() || commitHash == UNKNOWN_COMMIT_HASH -> CommitHashUi.Unavailable
    else -> CommitHashUi.Available(commitHash)
}
