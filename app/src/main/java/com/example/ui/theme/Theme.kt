package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GtColorScheme = darkColorScheme(
  primary = GtOrange,
  onPrimary = GtObsidian,
  primaryContainer = GtOrangeDark,
  onPrimaryContainer = GtTextPrimary,
  secondary = GtNeonCyan,
  onSecondary = GtObsidian,
  secondaryContainer = GtSlate,
  onSecondaryContainer = GtNeonCyan,
  tertiary = GtNeonYellow,
  background = GtObsidian,
  onBackground = GtTextPrimary,
  surface = GtCarbon,
  onSurface = GtTextPrimary,
  surfaceVariant = GtSlate,
  onSurfaceVariant = GtTextSecondary,
  outline = GtBorder,
  error = GtError
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = GtColorScheme,
    typography = Typography,
    content = content
  )
}
