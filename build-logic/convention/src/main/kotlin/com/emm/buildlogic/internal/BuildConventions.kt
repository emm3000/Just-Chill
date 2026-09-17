package com.emm.buildlogic.internal

import org.gradle.api.JavaVersion

internal object BuildConventions {
    const val COMPILE_SDK: Int = 37
    const val MIN_SDK: Int = 28
    const val TARGET_SDK: Int = 36
    const val JVM_TOOLCHAIN: Int = 17
    const val JVM_TARGET: String = "17"
    const val NAMESPACE_PREFIX: String = "com.emm.justchill"

    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17

    val COROUTINES_OPT_INS: List<String> = listOf(
        "kotlinx.coroutines.ExperimentalCoroutinesApi",
        "kotlinx.coroutines.FlowPreview",
    )

    val COMPOSE_OPT_INS: List<String> = listOf(
        "androidx.compose.material3.ExperimentalMaterial3Api",
        "androidx.compose.ui.ExperimentalComposeUiApi",
    )

    fun isGeneratedSource(invariantPath: String): Boolean = invariantPath.contains("/build/")

    fun namespaceOf(projectPath: String): String = projectPath
        .split(':', '-')
        .filter(String::isNotEmpty)
        .joinToString(separator = ".", prefix = "$NAMESPACE_PREFIX.")

    fun baselineNameOf(projectPath: String): String = projectPath
        .split(':')
        .filter(String::isNotEmpty)
        .joinToString(separator = "-")
}
