package io.github.kardeiro.gallery.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object GallerySpacing {
    val Tiny = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 24.dp
    val Section = 32.dp
}

object GalleryCorner {
    val Thumbnail = 18.dp
    val Card = 24.dp
    val Sheet = 32.dp
    val Pill = 100.dp
}

object GalleryMotion {
    const val Fast = 150
    const val Medium = 240
    const val Slow = 300
}

val GalleryShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
