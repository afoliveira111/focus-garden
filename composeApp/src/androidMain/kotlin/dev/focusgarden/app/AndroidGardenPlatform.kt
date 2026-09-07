package dev.focusgarden.app

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import java.time.LocalDate

class AndroidGardenPlatform(private val context: Context) : GardenPlatform {
    private val preferences = context.getSharedPreferences("focus_garden", Context.MODE_PRIVATE)

    override fun read(key: String): String? = preferences.getString(key, null)
    override fun write(key: String, value: String) { preferences.edit().putString(key, value).apply() }
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun todayKey(): String = LocalDate.now().toString()
    override fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
    override fun completionFeedback(enabled: Boolean) {
        Toast.makeText(context, "Seu bosque floresceu!", Toast.LENGTH_LONG).show()
        if (!enabled) return
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45).startTone(ToneGenerator.TONE_PROP_ACK, 500)
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") vibrator?.vibrate(180)
        }
    }
}
