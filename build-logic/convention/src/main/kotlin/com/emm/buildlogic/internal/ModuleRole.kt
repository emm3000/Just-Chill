package com.emm.buildlogic.internal

internal enum class ModuleRole {
    APP,
    CORE_DOMAIN,
    CORE,
    FEATURE,
    LAYER,
    ;

    fun allows(dependencyPath: String): Boolean = when (this) {
        APP -> true
        CORE_DOMAIN -> false
        CORE -> dependencyPath == CORE_DOMAIN_PATH
        FEATURE -> dependencyPath == CORE_DOMAIN_PATH || dependencyPath == CORE_UI_PATH
        LAYER -> !dependencyPath.startsWith(FEATURE_PREFIX)
    }

    fun rule(): String = when (this) {
        APP -> "the app composes every module"
        CORE_DOMAIN -> "$CORE_DOMAIN_PATH depends on no other module and stays JVM-only"
        CORE -> "a core module depends on $CORE_DOMAIN_PATH only"
        FEATURE -> "a feature depends on $CORE_DOMAIN_PATH and $CORE_UI_PATH only"
        LAYER -> "only $APP_PATH depends on a feature module"
    }

    companion object {
        const val APP_PATH: String = ":androidApp"
        const val CORE_DOMAIN_PATH: String = ":core:domain"
        const val CORE_UI_PATH: String = ":core:ui"

        private const val CORE_PREFIX: String = ":core:"
        private const val FEATURE_PREFIX: String = ":feature:"

        fun of(modulePath: String): ModuleRole = when {
            modulePath == APP_PATH -> APP
            modulePath == CORE_DOMAIN_PATH -> CORE_DOMAIN
            modulePath.startsWith(CORE_PREFIX) -> CORE
            modulePath.startsWith(FEATURE_PREFIX) -> FEATURE
            else -> LAYER
        }
    }
}
