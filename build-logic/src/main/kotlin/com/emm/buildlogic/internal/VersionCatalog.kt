package com.emm.buildlogic.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/**
 * Access to the SAME gradle/libs.versions.toml the main build uses. A Plugin<Project> class has no
 * `libs` accessor — that one is generated for .gradle.kts scripts only — so convention plugins look
 * the catalog up through this extension instead of hardcoding coordinates.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow {
        IllegalStateException("Version catalog entry '$alias' is missing from gradle/libs.versions.toml")
    }
