package jp.co.demo.marine.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = Color(0xFF27D3E2),
    onPrimary = Color(0xFF002126),
    secondary = Color(0xFFFFB547),
    background = Color(0xFF061521),
    surface = Color(0xFF102736),
    onSurface = Color(0xFFEAF8FA),
    error = Color(0xFFFF5B64)
)

@Composable
fun MarineTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = Scheme,
        content = content
    )
}
