package com.emm.justchill.hh.profile

/**
 * What `generate<Variant>BuildInfo` writes when git cannot answer — a source tarball, a shallow
 * export, no git on PATH.
 *
 * `GenerateBuildInfoTask.UNKNOWN_COMMIT` (build-logic, off the app's compile classpath) holds the
 * same word as a separate literal. `CommitHashUiTest` spells that word out and asserts this
 * constant classifies it as [CommitHashUi.Unavailable], so changing this value turns that test red.
 * Changing the generator's copy turns nothing red — build-logic has no test source set.
 */
const val UNKNOWN_COMMIT_HASH: String = "unknown"

/**
 * How much of the sha the footer shows. Git's own abbreviation floor, and what GitHub prints. The
 * screen always receives the full hash — the copy action needs all 40 characters — so this is the
 * one and only place the abbreviation happens.
 */
private const val SHORT_COMMIT_HASH_LENGTH = 7

/**
 * What the profile footer renders for the commit, as a state rather than a string.
 *
 * The two cases are not the same thing said differently: [Available] is an identifier the user can
 * hand to someone, [Unavailable] is the absence of one. Rendering the sentinel as if it were a hash
 * gave the user an English word inside a Spanish UI and a copy button that put the literal
 * `"unknown"` on the clipboard, confirmed by a snackbar claiming a hash had been copied.
 */
sealed interface CommitHashUi {

    /** Footer copy. Spanish, like all user-facing text; the sentinel and the ids stay English. */
    val label: String

    /** A real sha. [fullHash] is the whole 40 — [label] shows the first [SHORT_COMMIT_HASH_LENGTH]. */
    data class Available(val fullHash: String) : CommitHashUi {
        override val label: String = "Commit ${fullHash.take(SHORT_COMMIT_HASH_LENGTH)}"
    }

    /** Git could not answer at build time. Nothing to copy, so the footer offers no copy action. */
    data object Unavailable : CommitHashUi {
        override val label: String = "Commit no disponible"
    }
}

/**
 * Classifies what the platform layer injected. Blank counts as unavailable too: it is the
 * composable's own default, and an empty string is no more copyable than the sentinel.
 */
fun commitHashUi(commitHash: String): CommitHashUi = when {
    commitHash.isBlank() || commitHash == UNKNOWN_COMMIT_HASH -> CommitHashUi.Unavailable
    else -> CommitHashUi.Available(commitHash)
}
