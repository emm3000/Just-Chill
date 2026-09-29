package com.emm.justchill.core.preferences

import org.koin.mp.KoinPlatform

fun appPreferences(): AppPreferences = KoinPlatform.getKoin().get()
