package io.github.kardeiro.gallery.data.model

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MediaType {
    IMAGE, VIDEO
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val thumbUri: Uri,
    val mimeType: String,
    val dateTaken: Long,
    val bucketId: String,
    val bucketDisplayName: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val duration: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val mediaType: MediaType
        get() = if (mimeType.startsWith("video")) MediaType.VIDEO else MediaType.IMAGE

    val formattedDate: String
        get() = dateFormat.format(Date(dateTaken))

    companion object {
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    }

    val formattedSize: String
        get() {
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", size / 1024f)
                else -> String.format(Locale.US, "%.1f MB", size / (1024f * 1024f))
            }
        }
}

data class Album(
    val bucketId: String,
    val displayName: String,
    val coverUri: Uri,
    val itemCount: Int,
)
