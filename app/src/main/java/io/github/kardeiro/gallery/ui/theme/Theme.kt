package io.github.kardeiro.gallery.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = FallbackPrimary,
    onPrimary = FallbackOnPrimary,
    primaryContainer = FallbackPrimaryContainer,
    onPrimaryContainer = FallbackOnPrimaryContainer,
    secondary = FallbackSecondary,
    onSecondary = FallbackOnSecondary,
    secondaryContainer = FallbackSecondaryContainer,
    onSecondaryContainer = FallbackOnSecondaryContainer,
    tertiary = FallbackTertiary,
    onTertiary = FallbackOnTertiary,
    tertiaryContainer = FallbackTertiaryContainer,
    onTertiaryContainer = FallbackOnTertiaryContainer,
    background = FallbackBackground,
    onBackground = FallbackOnBackground,
    surface = FallbackSurface,
    onSurface = FallbackOnSurface,
    surfaceVariant = FallbackSurfaceVariant,
    onSurfaceVariant = FallbackOnSurfaceVariant,
    surfaceContainerLowest = FallbackSurfaceContainerLowest,
    surfaceContainerLow = FallbackSurfaceContainerLow,
    surfaceContainer = FallbackSurfaceContainer,
    surfaceContainerHigh = FallbackSurfaceContainerHigh,
    surfaceContainerHighest = FallbackSurfaceContainerHighest,
    outline = FallbackOutline,
    outlineVariant = FallbackOutlineVariant,
    scrim = FallbackScrim,
    error = FallbackError,
    onError = FallbackOnError,
    errorContainer = FallbackErrorContainer,
    onErrorContainer = FallbackOnErrorContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkFallbackPrimary,
    onPrimary = DarkFallbackOnPrimary,
    primaryContainer = DarkFallbackPrimaryContainer,
    onPrimaryContainer = DarkFallbackOnPrimaryContainer,
    secondary = DarkFallbackSecondary,
    onSecondary = DarkFallbackOnSecondary,
    secondaryContainer = DarkFallbackSecondaryContainer,
    onSecondaryContainer = DarkFallbackOnSecondaryContainer,
    tertiary = DarkFallbackTertiary,
    onTertiary = DarkFallbackOnTertiary,
    tertiaryContainer = DarkFallbackTertiaryContainer,
    onTertiaryContainer = DarkFallbackOnTertiaryContainer,
    background = DarkFallbackBackground,
    onBackground = DarkFallbackOnBackground,
    surface = DarkFallbackSurface,
    onSurface = DarkFallbackOnSurface,
    surfaceVariant = DarkFallbackSurfaceVariant,
    onSurfaceVariant = DarkFallbackOnSurfaceVariant,
    surfaceContainerLowest = DarkFallbackSurfaceContainerLowest,
    surfaceContainerLow = DarkFallbackSurfaceContainerLow,
    surfaceContainer = DarkFallbackSurfaceContainer,
    surfaceContainerHigh = DarkFallbackSurfaceContainerHigh,
    surfaceContainerHighest = DarkFallbackSurfaceContainerHighest,
    outline = DarkFallbackOutline,
    outlineVariant = DarkFallbackOutlineVariant,
    scrim = DarkFallbackScrim,
    error = DarkFallbackError,
    onError = DarkFallbackOnError,
    errorContainer = DarkFallbackErrorContainer,
    onErrorContainer = DarkFallbackOnErrorContainer,
)

@Composable
fun GalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GalleryTypography,
        shapes = GalleryShapes,
        content = content
    )
}
