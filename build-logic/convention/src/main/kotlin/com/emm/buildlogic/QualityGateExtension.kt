package com.emm.buildlogic

import org.gradle.api.provider.SetProperty

abstract class QualityGateExtension {

    abstract val detektTasks: SetProperty<String>
}
