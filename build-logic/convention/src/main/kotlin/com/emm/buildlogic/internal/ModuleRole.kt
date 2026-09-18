package com.emm.buildlogic.internal

internal enum class ModuleRole {
    ROOT,
    APP,
    CORE_DOMAIN,
    CORE,
    FEATURE,
    ;

    fun allows(dependencyPath: String): Boolean = when (this) {
        ROOT -> false
        APP -> true
        CORE_DOMAIN -> false
        CORE -> dependencyPath == CORE_DOMAIN_PATH
        FEATURE -> dependencyPath == CORE_DOMAIN_PATH || dependencyPath == CORE_UI_PATH
    }

    fun rule(): String = when (this) {
        ROOT -> "the root project depends on no module"
        APP -> "the app composes every module"
        CORE_DOMAIN -> "$CORE_DOMAIN_PATH depends on no other module and stays JVM-only"
        CORE -> "a core module depends on $CORE_DOMAIN_PATH only"
        FEATURE -> "a feature depends on $CORE_DOMAIN_PATH and $CORE_UI_PATH only"
    }

    companion object {
        const val ROOT_PATH: String = ":"
        const val APP_PATH: String = ":androidApp"
        const val CORE_DOMAIN_PATH: String = ":core:domain"
        const val CORE_UI_PATH: String = ":core:ui"
        const val CORE_TESTING_PATH: String = ":core:testing"

        private const val CORE_PREFIX: String = ":core:"
        private const val FEATURE_PREFIX: String = ":feature:"

        // ADR 015 left the root plus three families and no fallback role, so an unrecognised path
        // is a module nobody gave a boundary rule, not one the check should wave through.
        fun of(modulePath: String): ModuleRole = when {
            modulePath == ROOT_PATH -> ROOT
            modulePath == APP_PATH -> APP
            modulePath == CORE_DOMAIN_PATH -> CORE_DOMAIN
            modulePath.startsWith(CORE_PREFIX) -> CORE
            modulePath.startsWith(FEATURE_PREFIX) -> FEATURE
            else -> throw IllegalArgumentException(
                "$modulePath is neither $APP_PATH nor a $CORE_PREFIX or $FEATURE_PREFIX module",
            )
        }
    }
}
