package com.orbit.live

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val cBg = Color(0xFF07060F)
val cSurf = Color(0xFF14122A)
val Violet = Color(0xFF7C5CFF)
val Cyan = Color(0xFF00E5C3)
val Pink = Color(0xFFFF3D81)

@Composable
fun OrbitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet,
            secondary = Cyan,
            tertiary = Pink,
            background = cBg,
            surface = cSurf,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}
