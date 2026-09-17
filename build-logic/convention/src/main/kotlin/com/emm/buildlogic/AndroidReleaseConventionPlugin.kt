package com.emm.buildlogic

import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.api.dsl.ApplicationExtension
import com.emm.buildlogic.internal.gitOutput
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import java.util.Properties

class AndroidReleaseConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        apply<AndroidApplicationConventionPlugin>()
        pluginManager.apply("com.google.firebase.crashlytics")

        val extension: ApplicationExtension = extensions.getByType<ApplicationExtension>()
        extension.defaultConfig.versionCode = releaseVersionCode()
        extension.defaultConfig.versionName = releaseVersionName()
        configureReleaseSigning(extension, keystoreProperties())
        configureReleaseBuildType(extension)
    }

    private fun Project.releaseVersionCode(): Int =
        gitOutput("rev-list", "--count", "HEAD")?.toIntOrNull() ?: FALLBACK_VERSION_CODE

    // --match is not optional: the repo carries non-release tags (pre-kmp, post-s5) and a bare
    // describe returns whichever is nearest.
    private fun Project.releaseVersionName(): String =
        gitOutput("describe", "--tags", "--abbrev=0", "--match", "v[0-9]*")?.removePrefix("v") ?: FALLBACK_VERSION_NAME

    private fun Project.keystoreProperties(): Properties {
        val keystore: RegularFile = isolated.rootProject.projectDirectory.file(KEYSTORE_FILE)
        val content: String = providers.fileContents(keystore).asText.orNull.orEmpty()
        return Properties().apply { content.reader().use(::load) }
    }

    // Credentials are absent on fresh clones, forks and Dependabot runs; the release must still
    // configure and build unsigned there.
    private fun Project.configureReleaseSigning(extension: ApplicationExtension, keystore: Properties) {
        if (keystore.getProperty("keyAlias") == null) return
        extension.signingConfigs.create(RELEASE) {
            keyAlias = keystore.getProperty("keyAlias")
            keyPassword = keystore.getProperty("keyPassword")
            storeFile = file(keystore.getProperty("storeFile"))
            storePassword = keystore.getProperty("storePassword")
        }
    }

    private fun Project.configureReleaseBuildType(extension: ApplicationExtension) {
        val uploadsMapping: Boolean = providers.gradleProperty(MAPPING_UPLOAD_PROPERTY).orNull == "true"
        val release: ApplicationBuildType = extension.buildTypes.getByName(RELEASE)
        release.isMinifyEnabled = true
        release.isShrinkResources = true
        release.proguardFiles(extension.getDefaultProguardFile(DEFAULT_PROGUARD_FILE), PROGUARD_RULES)
        (release as ExtensionAware).extensions.configure<CrashlyticsExtension> {
            mappingFileUploadEnabled = uploadsMapping
        }
    }

    private companion object {
        const val RELEASE: String = "release"
        const val KEYSTORE_FILE: String = "keystore.properties"
        const val MAPPING_UPLOAD_PROPERTY: String = "justchill.crashlyticsMappingUpload"
        const val DEFAULT_PROGUARD_FILE: String = "proguard-android-optimize.txt"
        const val PROGUARD_RULES: String = "proguard-rules.pro"
        const val FALLBACK_VERSION_CODE: Int = 1
        const val FALLBACK_VERSION_NAME: String = "0.0.0-dev"
    }
}
