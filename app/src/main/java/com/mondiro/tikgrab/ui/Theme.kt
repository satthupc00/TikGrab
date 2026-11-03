package com.mondiro.tikgrab.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object C {
    val chrome = Color(0xFF1F2126)
    val panel = Color(0xFF26292F)
    val line = Color(0xFF363A42)
    val hover = Color(0xFF2F333A)
    val stage = Color(0xFF2B2D31)
    val text = Color(0xFFE8E9EC)
    val muted = Color(0xFF8D929C)
    val red = Color(0xFFE5383B)
    val redHi = Color(0xFFF04447)
    val selected = Color(0xFF3A2426)
    val green = Color(0xFF3DDC8A)
    val latest = Color(0xFF2EB872)
    val credit = Color(0xFF6B707A)
    val track = Color(0xFF3A3E46)
}

@Composable
fun TikGrabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.red, onPrimary = Color.White,
            secondary = C.red, onSecondary = Color.White,
            background = C.chrome, onBackground = C.text,
            surface = C.panel, onSurface = C.text,
            surfaceVariant = C.panel, onSurfaceVariant = C.muted,
            surfaceContainer = C.panel, surfaceContainerHigh = C.panel, surfaceContainerLow = C.panel,
            surfaceContainerHighest = C.hover, surfaceContainerLowest = C.chrome,
            outline = C.line, outlineVariant = C.line,
            error = C.red,
        ),
        content = content,
    )
}
