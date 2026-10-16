package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StiColorScheme = lightColorScheme(
  primary = StiBlue,
  onPrimary = Color.White,
  primaryContainer = StiBlueLight,
  onPrimaryContainer = Color.White,
  secondary = StiYellow,
  onSecondary = StiBlueDark,
  secondaryContainer = Color(0xFFFFF9C4),
  onSecondaryContainer = StiBlueDark,
  background = StiBackground,
  onBackground = StiTextPrimary,
  surface = StiSurface,
  onSurface = StiTextPrimary,
  surfaceVariant = Color(0xFFE2E8F0),
  onSurfaceVariant = StiTextSecondary,
  outline = Color(0xFFCBD5E1),
  error = StiError
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = StiColorScheme,
    typography = Typography,
    content = content
  )
}
