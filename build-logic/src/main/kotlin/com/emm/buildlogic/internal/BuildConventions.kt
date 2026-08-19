package com.emm.buildlogic.internal

/**
 * `minSdk` is deliberately absent: the modules genuinely disagree, and a shared default would
 * silently move one of them.
 */
internal object BuildConventions {
    const val COMPILE_SDK = 37
    const val JVM_TARGET = "17"

    fun isGeneratedSource(invariantPath: String): Boolean = invariantPath.contains("/build/")
}
