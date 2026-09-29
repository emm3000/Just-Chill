package com.emm.justchill.core.viewmodel

import com.emm.justchill.feature.profile.ProfileEffect
import com.emm.justchill.feature.profile.ProfileIntent
import com.emm.justchill.feature.profile.ProfileUiState
import com.emm.justchill.feature.profile.ProfileViewModel

fun profileViewModel(): MviHandle<ProfileUiState, ProfileIntent, ProfileEffect> = handleOf(ProfileViewModel::class)
