package com.emm.buildlogic.internal

/**
 * Project-wide build constants. These used to be copy-pasted into three module build files, so
 * bumping one meant editing all three and remembering all three.
 *
 * `minSdk` is deliberately absent: the modules genuinely disagree (26 for :domain and :data, 28 for
 * :shared-ui) and a shared default would hide a real difference.
 */
internal object BuildConventions {
    const val COMPILE_SDK = 37
    const val JVM_TARGET = "17"
}
