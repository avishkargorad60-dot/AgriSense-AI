package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AgriLightColorScheme = lightColorScheme(
  primary = ForestGreen,
  onPrimary = Color.White,
  primaryContainer = SoftSage,
  onPrimaryContainer = ForestGreen,
  secondary = FreshEmerald,
  onSecondary = Color.White,
  secondaryContainer = MintLeaf,
  onSecondaryContainer = ForestGreen,
  tertiary = HarvestGold,
  onTertiary = Color.White,
  tertiaryContainer = HarvestGoldContainer,
  onTertiaryContainer = EarthySoil,
  background = WarmCream,
  onBackground = TextPrimary,
  surface = WarmCream,
  onSurface = TextPrimary,
  surfaceVariant = SoftCreamSurface,
  onSurfaceVariant = TextSecondary,
  surfaceContainerLowest = CardCream,
  surfaceContainerLow = SoftCreamSurface,
  surfaceContainer = Color(0xFFECE7DA),
  outline = OutlineEarth,
  error = ErrorRed,
  onError = Color.White,
  errorContainer = ErrorContainerLight,
  onErrorContainer = ErrorRed,
)

private val AgriDarkColorScheme = darkColorScheme(
  primary = DarkMintPrimary,
  onPrimary = DarkForestBackground,
  primaryContainer = ForestGreen,
  onPrimaryContainer = MintLeaf,
  secondary = FreshEmerald,
  onSecondary = Color.White,
  secondaryContainer = DarkForestSurfaceVariant,
  onSecondaryContainer = DarkMintPrimary,
  tertiary = DarkHarvestGold,
  onTertiary = DarkForestBackground,
  tertiaryContainer = Color(0xFF473314),
  onTertiaryContainer = HarvestGoldContainer,
  background = DarkForestBackground,
  onBackground = DarkTextPrimary,
  surface = DarkForestBackground,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkForestSurface,
  onSurfaceVariant = DarkTextSecondary,
  surfaceContainerLowest = DarkForestSurface,
  surfaceContainerLow = DarkForestSurfaceVariant,
  surfaceContainer = Color(0xFF1D3024),
  outline = Color(0xFF3B5243),
)

val AgriSenseShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AgriSenseTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) AgriDarkColorScheme else AgriLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = AgriSenseTypography,
    shapes = AgriSenseShapes,
    content = content,
  )
}
