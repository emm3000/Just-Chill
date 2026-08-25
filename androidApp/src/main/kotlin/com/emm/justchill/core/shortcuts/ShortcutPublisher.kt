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
import com.emm.justchill.hh.transaction.GetSpendShortcutCombos
import com.emm.justchill.hh.transaction.ShortcutCombo

/**
 * Publishes the launcher's dynamic combo shortcuts (E09-03). The static `loans` shortcut is
 * declared in `shortcuts.xml`, not here, so it survives every call untouched.
 */
class ShortcutPublisher(private val context: Context, private val getShortcutCombos: GetSpendShortcutCombos) {

    suspend fun publish() {
        val shortcuts = getShortcutCombos().mapIndexed { rank, combo -> combo.toShortcutInfo(rank) }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
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
            .setShortLabel(title)
            .setLongLabel(subtitle)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_first_round))
            .setRank(rank)
            .setIntent(intent)
            .build()
    }
}
