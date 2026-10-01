package com.emm.justchill.core.ui.atoms

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class EmmSnackbarDurationTest {

    @Test
    fun `an error snackbar defaults to the long duration`() {
        val visuals: EmmSnackbarVisuals = EmmSnackbarVisuals(
            message = "No se pudo guardar",
            tone = EmmSnackbarTone.Error,
        )
        assertEquals(SnackbarDuration.Long, visuals.duration)
    }

    @Test
    fun `a success snackbar defaults to the short duration`() {
        val visuals: EmmSnackbarVisuals = EmmSnackbarVisuals(message = "Guardado", tone = EmmSnackbarTone.Success)
        assertEquals(SnackbarDuration.Short, visuals.duration)
    }

    @Test
    fun `an explicit duration wins over the tone default`() {
        val error: EmmSnackbarVisuals = EmmSnackbarVisuals(
            message = "No se pudo guardar",
            tone = EmmSnackbarTone.Error,
            duration = SnackbarDuration.Short,
        )
        val success: EmmSnackbarVisuals = EmmSnackbarVisuals(
            message = "Guardado",
            tone = EmmSnackbarTone.Success,
            duration = SnackbarDuration.Long,
        )
        assertEquals(SnackbarDuration.Short, error.duration)
        assertEquals(SnackbarDuration.Long, success.duration)
    }

    @Test
    fun `showing an error snackbar holds it for the long duration`() = runTest {
        assertEquals(SnackbarDuration.Long, shownDuration(EmmSnackbarTone.Error, duration = null))
    }

    @Test
    fun `showing a success snackbar holds it for the short duration`() = runTest {
        assertEquals(SnackbarDuration.Short, shownDuration(EmmSnackbarTone.Success, duration = null))
    }

    @Test
    fun `showing a snackbar with an explicit duration holds it for that duration`() = runTest {
        assertEquals(SnackbarDuration.Short, shownDuration(EmmSnackbarTone.Error, SnackbarDuration.Short))
        assertEquals(SnackbarDuration.Long, shownDuration(EmmSnackbarTone.Success, SnackbarDuration.Long))
    }

    private fun TestScope.shownDuration(tone: EmmSnackbarTone, duration: SnackbarDuration?): SnackbarDuration? {
        val hostState: SnackbarHostState = SnackbarHostState()
        launch {
            if (duration == null) {
                hostState.showEmmSnackbar(message = "Mensaje", tone = tone)
            } else {
                hostState.showEmmSnackbar(message = "Mensaje", tone = tone, duration = duration)
            }
        }
        runCurrent()
        val shown: SnackbarDuration? = hostState.currentSnackbarData?.visuals?.duration
        hostState.currentSnackbarData?.dismiss()
        runCurrent()
        return shown
    }
}
