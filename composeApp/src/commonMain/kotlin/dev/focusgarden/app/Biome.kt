package dev.focusgarden.app

import androidx.compose.ui.graphics.Color

enum class Biome(
    val title: String,
    val shortTitle: String,
    val symbol: String,
    val skyTop: Color,
    val skyBottom: Color,
    val ground: Color,
    val foliage: Color,
    val accent: Color,
) {
    SERENE("Mata de névoa", "Mata", "●", Color(0xFF6E9C93), Color(0xFFD5CDAF), Color(0xFF466448), Color(0xFF66855D), Color(0xFFF5C987)),
    SAKURA("Vale das cerejeiras", "Vale", "○", Color(0xFF7E8EB1), Color(0xFFF0C9C5), Color(0xFF52664F), Color(0xFFE9AEB6), Color(0xFFFFE0E3)),
    NIGHT("Lago alpino", "Lago", "✦", Color(0xFF101C36), Color(0xFF374A5E), Color(0xFF172D2B), Color(0xFF345947), Color(0xFFEBD66F)),
    AUTUMN("Planalto de outono", "Planalto", "◆", Color(0xFF7B7C7A), Color(0xFFE4B27E), Color(0xFF574A35), Color(0xFFB8663C), Color(0xFFE6A354)),
}
