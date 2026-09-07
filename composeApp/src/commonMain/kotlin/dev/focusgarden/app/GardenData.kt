package dev.focusgarden.app

data class GardenRecord(
    val timestamp: Long,
    val epochDay: Long,
    val dateKey: String,
    val durationMinutes: Int,
    val biome: Biome,
)

data class GardenSettings(
    val randomBiome: Boolean = false,
    val soundEnabled: Boolean = true,
    val durationMinutes: Int = 25,
)

interface GardenPlatform {
    fun read(key: String): String?
    fun write(key: String, value: String)
    fun nowMillis(): Long
    fun todayKey(): String
    fun todayEpochDay(): Long
    fun completionFeedback(enabled: Boolean)
}

object GardenRuntime {
    lateinit var platform: GardenPlatform
}

object GardenDataStore {
    private const val RecordsKey = "garden_records_v1"
    private const val SettingsKey = "garden_settings_v1"

    fun loadRecords(): List<GardenRecord> = GardenRuntime.platform.read(RecordsKey)
        .orEmpty().lineSequence().mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size != 5) return@mapNotNull null
            GardenRecord(
                timestamp = parts[0].toLongOrNull() ?: return@mapNotNull null,
                epochDay = parts[1].toLongOrNull() ?: return@mapNotNull null,
                dateKey = parts[2],
                durationMinutes = parts[3].toIntOrNull() ?: return@mapNotNull null,
                biome = Biome.entries.firstOrNull { it.name == parts[4] } ?: return@mapNotNull null,
            )
        }.sortedByDescending { it.timestamp }.toList()

    fun addRecord(durationMinutes: Int, biome: Biome): List<GardenRecord> {
        val updated = listOf(
            GardenRecord(
                GardenRuntime.platform.nowMillis(), GardenRuntime.platform.todayEpochDay(),
                GardenRuntime.platform.todayKey(), durationMinutes, biome,
            ),
        ) + loadRecords()
        GardenRuntime.platform.write(
            RecordsKey,
            updated.take(200).joinToString("\n") {
                "${it.timestamp}|${it.epochDay}|${it.dateKey}|${it.durationMinutes}|${it.biome.name}"
            },
        )
        return updated.take(200)
    }

    fun loadSettings(): GardenSettings {
        val values = GardenRuntime.platform.read(SettingsKey).orEmpty().split('|')
        if (values.size != 3) return GardenSettings()
        return GardenSettings(
            randomBiome = values[0].toBooleanStrictOrNull() ?: false,
            soundEnabled = values[1].toBooleanStrictOrNull() ?: true,
            durationMinutes = values[2].toIntOrNull()?.coerceIn(1, 180) ?: 25,
        )
    }

    fun saveSettings(settings: GardenSettings) {
        GardenRuntime.platform.write(
            SettingsKey,
            "${settings.randomBiome}|${settings.soundEnabled}|${settings.durationMinutes}",
        )
    }
}

internal fun currentStreak(records: List<GardenRecord>): Int {
    if (records.isEmpty()) return 0
    val days = records.map { it.epochDay }.distinct().sortedDescending()
    val today = GardenRuntime.platform.todayEpochDay()
    if (days.first() < today - 1) return 0
    var streak = 1
    for (index in 1 until days.size) {
        if (days[index - 1] - days[index] != 1L) break
        streak++
    }
    return streak
}
