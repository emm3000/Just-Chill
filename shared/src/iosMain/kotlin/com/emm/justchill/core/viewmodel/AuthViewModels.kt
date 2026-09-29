package com.emm.justchill.core.viewmodel

import com.emm.justchill.feature.auth.AuthEffect
import com.emm.justchill.feature.auth.AuthIntent
import com.emm.justchill.feature.auth.AuthUiState
import com.emm.justchill.feature.auth.AuthViewModel

fun resolveAuthHandle(): MviHandle<AuthUiState, AuthIntent, AuthEffect> = handleOf(AuthViewModel::class)
