package io.github.kardeiro.gallery.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import io.github.kardeiro.gallery.data.model.Album
import io.github.kardeiro.gallery.data.model.MediaItem

class MediaRepository(private val context: Context) {

    fun loadMedia(): List<MediaItem> {
        val images = queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)
        val videos = queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DURATION)
        return (images + videos).sortedByDescending { it.dateTaken }
    }

    private fun queryMedia(uri: Uri, durationColName: String?): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.LATITUDE,
            MediaStore.Images.Media.LONGITUDE,
        )
        if (durationColName != null) projection.add(durationColName)

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        context.contentResolver.query(
            uri,
            projection.toTypedArray(),
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val latCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LATITUDE)
            val lngCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LONGITUDE)
            val durationCol = durationColName?.let { cursor.getColumnIndexOrThrow(it) }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(uri, id)

                items.add(
                    MediaItem(
                        id = id,
                        uri = contentUri,
                        thumbUri = contentUri,
                        mimeType = cursor.getString(mimeCol),
                        dateTaken = cursor.getLong(dateCol),
                        bucketId = cursor.getString(bucketIdCol),
                        bucketDisplayName = cursor.getString(bucketNameCol),
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        size = cursor.getLong(sizeCol),
                        latitude = if (!cursor.isNull(latCol)) cursor.getDouble(latCol) else null,
                        longitude = if (!cursor.isNull(lngCol)) cursor.getDouble(lngCol) else null,
                        duration = durationCol?.let { col ->
                            if (!cursor.isNull(col)) cursor.getLong(col) else null
                        },
                    )
                )
            }
        }

        return items
    }

    fun loadAlbums(): List<Album> {
        val albumMap = mutableMapOf<String, Album>()

        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media._ID,
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdCol)
                val displayName = cursor.getString(bucketNameCol)
                val coverId = cursor.getLong(idCol)
                val coverUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, coverId
                )

                val existing = albumMap[bucketId]
                if (existing == null) {
                    albumMap[bucketId] = Album(
                        bucketId = bucketId,
                        displayName = displayName,
                        coverUri = coverUri,
                        itemCount = 1,
                    )
                } else {
                    albumMap[bucketId] = existing.copy(
                        itemCount = existing.itemCount + 1
                    )
                }
            }
        }

        return albumMap.values.toList()
    }

    fun deleteMedia(uri: Uri): Boolean {
        return try {
            context.contentResolver.delete(uri, null, null) > 0
        } catch (_: SecurityException) {
            false
        }
    }
}
