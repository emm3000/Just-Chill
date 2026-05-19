package com.emm.justchill.hh.profile

import com.emm.domain.shared.backup.ExportDataUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.BuildConfig
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import java.io.OutputStream

class ProfileViewModel(
    private val exportData: ExportDataUseCase,
    private val importData: ImportDataUseCase,
) : MviViewModel<ProfileUiState, ProfileIntent, ProfileEffect>() {

    override val initialState = ProfileUiState()

    override fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.ExportToStream -> exportToStream(intent.output)
            is ProfileIntent.ImportJson -> importFromJson(intent.json)
        }
    }

    private fun exportToStream(output: OutputStream) = launchSafe(
        onError = { e ->
            ProfileEffect.ShowMessage(e.toUserMessage())
        },
    ) {
        updateState { copy(isExporting = true) }
        try {
            val json = exportData(
                exportedAt = System.currentTimeMillis(),
                appVersion = BuildConfig.VERSION_NAME,
            )
            // Writer is closed here — not at the launcher callsite — because the launcher
            // hands us a raw stream and we schedule async work; closing it early would corrupt the write.
            // Closing the BufferedWriter flushes its buffer to the stream before closing.
            output.bufferedWriter().use { it.write(json) }
            sendEffect(ProfileEffect.ShowMessage("Listo, tu data está guardada."))
        } catch (e: DomainException) {
            sendEffect(ProfileEffect.ShowMessage(e.toUserMessage()))
        } catch (e: Exception) {
            sendEffect(ProfileEffect.ShowMessage("No pude exportar — capaz no hay espacio en tu celu?"))
        } finally {
            updateState { copy(isExporting = false) }
        }
    }

    private fun importFromJson(json: String) = launchSafe(
        onError = { e ->
            ProfileEffect.ShowMessage(e.toUserMessage())
        },
    ) {
        updateState { copy(isImporting = true) }
        try {
            val stats = importData(json)
            sendEffect(ProfileEffect.ShowMessage("Listo — ${stats.transactions} movimientos importados."))
        } catch (e: DomainException) {
            sendEffect(ProfileEffect.ShowMessage(e.toUserMessage()))
        } catch (e: Exception) {
            sendEffect(ProfileEffect.ShowMessage("No pude importar el archivo — capaz está dañado."))
        } finally {
            updateState { copy(isImporting = false) }
        }
    }
}
