package jp.co.demo.taxi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TaxiColors = darkColorScheme(
    primary = Color(0xFFFFD21F),
    onPrimary = Color(0xFF111318),
    secondary = Color(0xFFFFE990),
    background = Color(0xFF111318),
    surface = Color(0xFF1B1E24),
    onSurface = Color(0xFFF7F3E8)
)

@Composable
fun TaxiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TaxiColors, content = content)
}
