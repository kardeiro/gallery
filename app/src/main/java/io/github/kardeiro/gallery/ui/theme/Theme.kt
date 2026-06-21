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
    background = FallbackBackground,
    onBackground = FallbackOnBackground,
    surface = FallbackSurface,
    onSurface = FallbackOnSurface,
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
        darkTheme -> darkColorScheme()
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GalleryTypography,
        content = content
    )
}
