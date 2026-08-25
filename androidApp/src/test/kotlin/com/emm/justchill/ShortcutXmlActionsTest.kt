package com.emm.justchill

import com.emm.justchill.hh.shared.ACTION_OPEN_LOANS
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals

class ShortcutXmlActionsTest {

    @Test
    fun `main shortcuts xml pins the loans action to ACTION_OPEN_LOANS`() {
        assertEquals(ACTION_OPEN_LOANS, loansShortcutAction(File("src/main/res/xml/shortcuts.xml")))
    }

    @Test
    fun `dev shortcuts xml pins the loans action to ACTION_OPEN_LOANS`() {
        assertEquals(ACTION_OPEN_LOANS, loansShortcutAction(File("src/dev/res/xml/shortcuts.xml")))
    }

    private fun loansShortcutAction(file: File): String? {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val shortcuts = document.getElementsByTagName("shortcut")
        for (index in 0 until shortcuts.length) {
            val shortcut = shortcuts.item(index) as Element
            if (shortcut.getAttribute("android:shortcutId") != "loans") continue
            val intent = shortcut.getElementsByTagName("intent").item(0) as Element
            return intent.getAttribute("android:action")
        }
        return null
    }
}
