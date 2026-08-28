package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class ColorPreset(
    val name: String,
    val hex: String,
    val primary: Color,
    val secondary: Color
)

val ColorPresets = listOf(
    ColorPreset("Azul Político", "#1E3A8A", Color(0xFF1E3A8A), Color(0xFF2563EB)),
    ColorPreset("Verde Esmeralda", "#065F46", Color(0xFF065F46), Color(0xFF059669)),
    ColorPreset("Rojo Borgoña", "#991B1B", Color(0xFF991B1B), Color(0xFFDC2626)),
    ColorPreset("Púrpura Imperial", "#581C87", Color(0xFF581C87), Color(0xFF7C3AED)),
    ColorPreset("Slate Elegante", "#334155", Color(0xFF334155), Color(0xFF475569)),
    ColorPreset("Dorado Presidencial", "#854D0E", Color(0xFF854D0E), Color(0xFFCA8A04))
)

fun parseHexColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        Color(0xFF1E3A8A)
    }
}
