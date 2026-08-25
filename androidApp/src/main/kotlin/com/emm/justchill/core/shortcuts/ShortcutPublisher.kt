package com.emm.justchill.core.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.emm.justchill.MainActivity
import com.emm.justchill.R
import com.emm.justchill.hh.shared.ACTION_ADD_TRANSACTION
import com.emm.justchill.hh.shared.EXTRA_ACCOUNT_ID
import com.emm.justchill.hh.shared.EXTRA_CATEGORY_ID
import com.emm.justchill.hh.shared.EXTRA_TYPE
import com.emm.justchill.hh.transaction.GetShortcutCombos
import com.emm.justchill.hh.transaction.ShortcutCombo
import kotlinx.coroutines.CancellationException

/**
 * Publishes the launcher's dynamic combo shortcuts, replacing the whole set every call so a combo
 * that fell out of the ranking disappears (E09-03). `setDynamicShortcuts`, never
 * `pushDynamicShortcut` — the static `loans` shortcut is declared in `shortcuts.xml`, not here, so
 * it survives untouched.
 */
class ShortcutPublisher(private val context: Context, private val getShortcutCombos: GetShortcutCombos) {

    // The launcher is a convenience: a stats read or a shortcut-manager call failing must never
    // crash the app that called this off the cold-start path. CancellationException is rethrown so
    // a caller that cancels this job is still able to.
    @Suppress("TooGenericExceptionCaught")
    suspend fun publish() {
        try {
            val shortcuts = getShortcutCombos().mapIndexed { rank, combo -> combo.toShortcutInfo(rank) }
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    // No `Intent.apply { }`: Intent's own `type` property (the MIME type) would shadow
    // ShortcutCombo.type inside that lambda's implicit receiver and silently extra a null.
    private fun ShortcutCombo.toShortcutInfo(rank: Int): ShortcutInfoCompat {
        val intent = Intent(context, MainActivity::class.java)
        intent.action = ACTION_ADD_TRANSACTION
        intent.putExtra(EXTRA_ACCOUNT_ID, accountId)
        intent.putExtra(EXTRA_CATEGORY_ID, categoryId)
        intent.putExtra(EXTRA_TYPE, type)
        return ShortcutInfoCompat.Builder(context, "combo-$accountId-$categoryId-$type")
            .setShortLabel(shortLabel)
            .setLongLabel(longLabel)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_first_round))
            .setRank(rank)
            .setIntent(intent)
            .build()
    }
}
