package com.emm.buildlogic.internal

import com.android.build.api.dsl.CommonExtension
import com.emm.buildlogic.QualityGateConventionPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal fun Project.configureKotlin(optIns: List<String>) {
    extensions.configure<KotlinBaseExtension> {
        jvmToolchain(BuildConventions.JVM_TOOLCHAIN)
    }
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.optIn.addAll(optIns)
    }
}

internal fun Project.configureKotlinAndroid(extension: CommonExtension) {
    extension.compileSdk = BuildConventions.COMPILE_SDK
    extension.defaultConfig.minSdk = BuildConventions.MIN_SDK
    extension.compileOptions.sourceCompatibility = BuildConventions.JAVA_VERSION
    extension.compileOptions.targetCompatibility = BuildConventions.JAVA_VERSION
    configureKotlin(BuildConventions.COROUTINES_OPT_INS)
}

internal fun Project.configureAndroidUnitTestDependencies() {
    dependencies {
        add("testImplementation", kotlin("test-junit"))
        add("testImplementation", libs.library("junit"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
        add("testImplementation", libs.library("mockk"))
    }
}

internal fun Project.configureAndroidCompose(extension: CommonExtension) {
    extension.buildFeatures.compose = true

    dependencies {
        add("implementation", platform(libs.library("androidx-compose-bom")))
        add("implementation", libs.library("androidx-runtime"))
        add("implementation", libs.library("androidx-foundation"))
        add("implementation", libs.library("androidx-ui"))
        add("implementation", libs.library("androidx-ui-tooling-preview"))
        add("implementation", libs.library("androidx-material3"))
        add("debugImplementation", libs.library("androidx-ui-tooling"))
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.optIn.addAll(BuildConventions.COMPOSE_OPT_INS)
    }
}

internal fun Project.gateOn(testTask: String) {
    tasks.named(QualityGateConventionPlugin.GATE_TASK) {
        dependsOn(testTask)
    }
}
