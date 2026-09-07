package dev.focusgarden.app

import kotlinx.browser.document
import kotlinx.browser.localStorage

@JsFun("() => Date.now()")
private external fun browserNow(): Double

@JsFun("(millis) => new Date(millis).toISOString().slice(0, 10)")
private external fun browserDateKey(millis: Double): JsString

class WebGardenPlatform : GardenPlatform {
    override fun read(key: String): String? = localStorage.getItem(key)
    override fun write(key: String, value: String) = localStorage.setItem(key, value)
    override fun nowMillis(): Long = browserNow().toLong()
    override fun todayKey(): String = browserDateKey(browserNow()).toString()
    override fun todayEpochDay(): Long = (browserNow() / 86_400_000.0).toLong()
    override fun completionFeedback(enabled: Boolean) {
        document.title = "Seu bosque floresceu! · Focus Garden"
    }
}
