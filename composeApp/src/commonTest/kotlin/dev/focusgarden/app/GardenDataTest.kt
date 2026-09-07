package dev.focusgarden.app

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GardenDataTest {
    private lateinit var platform: FakeGardenPlatform

    @BeforeTest
    fun setup() {
        platform = FakeGardenPlatform()
        GardenRuntime.platform = platform
    }

    @Test
    fun completedSessionIsPersisted() {
        val records = GardenDataStore.addRecord(25, Biome.SAKURA)
        assertEquals(1, records.size)
        assertEquals(25, GardenDataStore.loadRecords().single().durationMinutes)
        assertEquals(Biome.SAKURA, GardenDataStore.loadRecords().single().biome)
    }

    @Test
    fun settingsRoundTrip() {
        val expected = GardenSettings(randomBiome = true, soundEnabled = false, durationMinutes = 40)
        GardenDataStore.saveSettings(expected)
        assertEquals(expected, GardenDataStore.loadSettings())
    }

    @Test
    fun streakCountsOnlyConsecutiveDays() {
        val records = listOf(
            GardenRecord(3, 100, "2026-08-04", 25, Biome.SERENE),
            GardenRecord(2, 99, "2026-08-03", 25, Biome.SAKURA),
            GardenRecord(1, 97, "2026-08-01", 25, Biome.NIGHT),
        )
        assertEquals(2, currentStreak(records))
    }

    private class FakeGardenPlatform : GardenPlatform {
        private val values = mutableMapOf<String, String>()
        override fun read(key: String): String? = values[key]
        override fun write(key: String, value: String) { values[key] = value }
        override fun nowMillis(): Long = 1234
        override fun todayKey(): String = "2026-08-04"
        override fun todayEpochDay(): Long = 100
        override fun completionFeedback(enabled: Boolean) = Unit
    }
}
