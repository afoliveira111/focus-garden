package dev.focusgarden.app

import java.awt.Toolkit
import java.time.LocalDate
import java.util.prefs.Preferences

class DesktopGardenPlatform : GardenPlatform {
    private val preferences = Preferences.userRoot().node("dev/focusgarden/app")

    override fun read(key: String): String? = preferences.get(key, null)
    override fun write(key: String, value: String) = preferences.put(key, value)
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun todayKey(): String = LocalDate.now().toString()
    override fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
    override fun completionFeedback(enabled: Boolean) {
        if (enabled) Toolkit.getDefaultToolkit().beep()
    }
}
