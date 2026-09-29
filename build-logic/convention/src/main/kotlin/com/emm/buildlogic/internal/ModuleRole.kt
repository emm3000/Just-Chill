package com.emm.buildlogic.internal

internal enum class ModuleRole {
    ROOT,
    APP,
    SHARED,
    CORE_DOMAIN,
    CORE_PRESENTATION,
    CORE_UI,
    CORE,
    FEATURE,
    ;

    fun allows(dependencyPath: String): Boolean = when (this) {
        ROOT -> false
        APP -> true
        SHARED ->
            (dependencyPath.startsWith(CORE_PREFIX) || dependencyPath.startsWith(FEATURE_PREFIX)) &&
                dependencyPath != CORE_TESTING_PATH
        CORE_DOMAIN -> false
        CORE_PRESENTATION -> dependencyPath == CORE_DOMAIN_PATH
        CORE_UI -> dependencyPath == CORE_DOMAIN_PATH || dependencyPath == CORE_PRESENTATION_PATH
        CORE -> dependencyPath == CORE_DOMAIN_PATH
        FEATURE ->
            dependencyPath == CORE_DOMAIN_PATH ||
                dependencyPath == CORE_PRESENTATION_PATH ||
                dependencyPath == CORE_UI_PATH
    }

    fun rule(): String = when (this) {
        ROOT -> "the root project depends on no module"
        APP -> "the app composes every module"
        SHARED -> "$SHARED_PATH depends on $CORE_PREFIX and $FEATURE_PREFIX modules only, $CORE_TESTING_PATH in tests alone"
        CORE_DOMAIN -> "$CORE_DOMAIN_PATH depends on no other module and keeps its production sources in commonMain"
        CORE_PRESENTATION -> "$CORE_PRESENTATION_PATH depends on $CORE_DOMAIN_PATH only"
        CORE_UI -> "$CORE_UI_PATH depends on $CORE_DOMAIN_PATH and $CORE_PRESENTATION_PATH only"
        CORE -> "a core module depends on $CORE_DOMAIN_PATH only"
        FEATURE -> "a feature depends on $CORE_DOMAIN_PATH, $CORE_PRESENTATION_PATH and $CORE_UI_PATH only"
    }

    companion object {
        const val ROOT_PATH: String = ":"
        const val APP_PATH: String = ":androidApp"
        const val SHARED_PATH: String = ":shared"
        const val CORE_DOMAIN_PATH: String = ":core:domain"
        const val CORE_PRESENTATION_PATH: String = ":core:presentation"
        const val CORE_UI_PATH: String = ":core:ui"
        const val CORE_TESTING_PATH: String = ":core:testing"

        private const val CORE_PREFIX: String = ":core:"
        private const val FEATURE_PREFIX: String = ":feature:"

        // No fallback role: an unrecognised path is a module nobody gave a boundary rule, not one
        // the check should wave through.
        fun of(modulePath: String): ModuleRole = when {
            modulePath == ROOT_PATH -> ROOT
            modulePath == APP_PATH -> APP
            modulePath == SHARED_PATH -> SHARED
            modulePath == CORE_DOMAIN_PATH -> CORE_DOMAIN
            modulePath == CORE_PRESENTATION_PATH -> CORE_PRESENTATION
            modulePath == CORE_UI_PATH -> CORE_UI
            modulePath.startsWith(CORE_PREFIX) -> CORE
            modulePath.startsWith(FEATURE_PREFIX) -> FEATURE
            else -> throw IllegalArgumentException(
                "$modulePath is neither $APP_PATH, $SHARED_PATH nor a $CORE_PREFIX or $FEATURE_PREFIX module",
            )
        }
    }
}
