package jp.co.demo.endoscope.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = Color(0xFF32D7E6),
    onPrimary = Color(0xFF001F24),
    secondary = Color(0xFF8BE8F0),
    background = Color(0xFF07131F),
    surface = Color(0xFF102231),
    onSurface = Color(0xFFEAF7F8),
    error = Color(0xFFFF4D5A)
)

@Composable
fun EndoscopeTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, content = content)
