package com.emm.justchill.hh.profile

/**
 * Koin qualifier for the full 40-char sha the build came from.
 *
 * A constant, not a literal at each site, because the two sites live in different Gradle modules:
 * `androidPlatformModule` (`:androidApp`) binds it and `AppNavHost` (this module) resolves it. As
 * two literals a rename on either side compiled clean and crashed the app at launch; referenced
 * from here, a rename cannot compile. `:androidApp` depends on `:ui-android`, so this is the only
 * place both can see.
 */
const val COMMIT_HASH_QUALIFIER: String = "commitHash"

/**
 * What `generate<Variant>BuildInfo` writes when git cannot answer — a source tarball, a shallow
 * export, no git on PATH. Kept in sync with `GenerateBuildInfoTask.UNKNOWN_COMMIT` by
 * `BuildInfoTest`, which asserts the shipped value is this word or a 40-hex sha.
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
