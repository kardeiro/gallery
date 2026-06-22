# Plan 003: Add video support to gallery — query videos, show badges, play in viewer

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat 33c77a9..HEAD -- app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt app/build.gradle.kts gradle/libs.versions.toml`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none
- **Category**: direction
- **Planned at**: commit `33c77a9`, 2026-06-22

## Why this matters

The app declares `READ_MEDIA_VIDEO` permission, includes `coil-video` for
loading video frames, and defines a `MediaType.VIDEO` enum — but it never
actually queries videos from the device. Users with video files see none
of them. This plan completes the half-built video support: add videos to
the media feed, show a duration badge on video thumbnails, and play videos
in the viewer instead of showing a static image.

## Current state

### Files and their roles

- `app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt` —
  media queries; currently only queries `MediaStore.Images.Media`
- `app/src/main/java/io/github/kardeiro/gallery/data/model/MediaItem.kt` —
  data model; `MediaType.VIDEO` already defined, `duration` field exists
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt` —
  thumbnail grid; no video badge overlay
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt` —
  full-screen viewer; always uses `ZoomableImage`, no video playback
- `app/build.gradle.kts` — dependencies; needs Media3 ExoPlayer for playback
- `gradle/libs.versions.toml` — version catalog; needs Media3 version entry

### Existing relevant code

**MediaRepository.kt** — `loadMedia()` only queries images:
```kotlin
// line 16-20
val collection = if (Environment.isExternalStorageLegacy()) {
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
} else {
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
}
```

**MediaItem.kt** — `duration` field and `MediaType` ready:
```kotlin
// line 8-10
enum class MediaType {
    IMAGE, VIDEO
}

// line 23
val duration: Long? = null,
```

**GalleryScreen.kt** — thumbnail without video indicator:
```kotlin
// lines 224-234
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(item.uri)
        .size(360)
        .crossfade(true)
        .build(),
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = Modifier.fillMaxSize()
)
```

**ViewerScreen.kt** — always `ZoomableImage`:
```kotlin
// line 126-127
val item = mediaItems[page]
ZoomableImage(item = item)
```

**AndroidManifest.xml** — permission already declared:
```xml
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
```

### Repo conventions

- Imports: `android.*`, then `androidx.*`, then `coil.*`, then
  `io.github.kardeiro.*`, then `kotlinx.*`
- Compose view composition: private composables at bottom of file, params
  as function args, no default exports
- Strings: always via `stringResource(R.string.xxx)` with `import io.github.kardeiro.gallery.R`
- Duration formatting: use `String.format(Locale.US, ...)` pattern matching
  existing `formattedSize` in `MediaItem.kt:39-42`

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Build APK | `gradle assembleDebug` | `BUILD SUCCESSFUL`, exit 0 |

Note on environment: if `gradle` is not available, verify via static
analysis (grep for expected patterns, check imports, verify no
compilation-breaking references). The real build happens on CI.

## Scope

**In scope** (the only files you should modify):
- `gradle/libs.versions.toml` — add media3 version + library entry
- `app/build.gradle.kts` — add media3-exoplayer dependency
- `app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt` — query videos in loadMedia
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt` — add video badge overlay
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt` — handle video playback

**Out of scope** (do NOT touch):
- `NavGraph.kt` — no new route needed; reuse existing VIEWER route
- `AlbumScreen.kt`, `AlbumDetailScreen.kt` — video support in album view is deferred
- `Theme.kt`, `Color.kt`, `Type.kt` — not related
- `strings.xml` — no new strings needed for this feature

## Git workflow

- Branch: `advisor/003-video-support`
- Commit message style: `feat: add video querying, badge, and playback`

## Steps

### Step 1: Add Media3 dependency to version catalog

Open `gradle/libs.versions.toml`.

Add a version entry after the existing `exifinterface = "1.3.7"` line:
```toml
media3 = "1.5.1"
```

Add a library entry after the existing `androidx-exifinterface` line:
```toml
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
```

**Verify**:
```bash
grep -E 'media3' gradle/libs.versions.toml
```
→ Shows one version and one library entry.

### Step 2: Add Media3 dependency to app module

Open `app/build.gradle.kts`.

Add after the existing `implementation(libs.androidx.exifinterface)` line:
```kotlin
    implementation(libs.androidx.media3.exoplayer)
```

**Verify**:
```bash
grep 'media3' app/build.gradle.kts
```
→ Shows `implementation(libs.androidx.media3.exoplayer)`

### Step 3: Update MediaRepository to query videos

Open `app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt`.

Replace the existing `loadMedia()` function to return a merged, date-sorted
list of both images and videos. The function should:

1. Query `MediaStore.Images.Media` as before (keep existing code)
2. Query `MediaStore.Video.Media` using the same projection columns
3. For videos, also read `MediaStore.Video.Media.DURATION`
4. For videos, set the `MediaItem.mimeType` from the cursor and let
   `mediaType` property determine IMAGE vs VIDEO
5. Merge both lists and sort by `dateTaken DESC`
6. Use a private helper `queryMedia(uri, durationCol)` to avoid duplication

Replace the entire `loadMedia()` function body:

```kotlin
fun loadMedia(): List<MediaItem> {
    val images = queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)
    val videos = queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DURATION)
    return (images + videos).sortedByDescending { it.dateTaken }
}

private fun queryMedia(uri: Uri, durationColName: String?): List<MediaItem> {
    val items = mutableListOf<MediaItem>()

    val projection = arrayOf(
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

    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

    context.contentResolver.query(
        uri,
        projection,
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
```

Keep the `loadAlbums()` and `deleteMedia()` methods unchanged.

**Verify**:
```bash
grep -n 'queryMedia\|Video.Media\|duration' app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt
```
→ Shows `queryMedia` called with both Images and Video URIs, and DURATION column referenced.

Also verify the file compiles by checking there are no unresolved references:
```bash
grep -n 'import.*Uri' app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt | head -1
```
→ Shows `import android.net.Uri` (already present).

### Step 4: Add video duration badge to Gallery thumbnails

Open `app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt`.

The `coil-video` module (already in deps) will automatically load video
frames as static images via `AsyncImage`. You just need to add a duration
badge overlay for video items.

In `GalleryScreen.kt`, find the `MediaThumbnail` composable (line 211).
Modify the `Box` content to conditionally show a duration overlay when
the item's `mediaType == MediaType.VIDEO` and `duration != null`.

Add the import at the top (after existing `import coil.*` lines):
```kotlin
import io.github.kardeiro.gallery.data.model.MediaType
```

Modify the `MediaThumbnail` composable's Box to add an overlay at the
bottom-right corner:

```kotlin
@Composable
private fun MediaThumbnail(
    item: MediaItem,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .size(360)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (item.mediaType == MediaType.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(item.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
```

Add a private helper function at the bottom of the file (before the final `}`):
```kotlin
private fun formatDuration(durationMs: Long?): String {
    if (durationMs == null || durationMs <= 0) return ""
    val totalSeconds = (durationMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
```

Add the new import at the top of the file:
```kotlin
import java.util.Locale
```

**Verify**:
```bash
grep -n 'MediaType.VIDEO\|formatDuration' app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt
```
→ Shows MediaType.VIDEO check and formatDuration function.

Also verify no new hardcoded strings were introduced:
```bash
grep -n '"' app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt | grep -v 'stringResource\|R\.'
```
→ The only strings should be imports, annotations, and the empty string in `formatDuration`.

### Step 5: Add video playback to ViewerScreen

Open `app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`.

Add imports at the top after existing `import androidx.compose.*` lines:
```kotlin
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
```

Add import for the model:
```kotlin
import io.github.kardeiro.gallery.data.model.MediaType
```

In the `HorizontalPager` content (line 120-128), replace:
```kotlin
{ page ->
    val item = mediaItems[page]
    ZoomableImage(item = item)
}
```

With:
```kotlin
{ page ->
    val item = mediaItems[page]
    if (item.mediaType == MediaType.VIDEO) {
        VideoPlayer(item = item, isVisible = page == pagerState.currentPage)
    } else {
        ZoomableImage(item = item)
    }
}
```

Add the `VideoPlayer` composable at the bottom of the file (after
`ZoomableImage`), following the same private-composable pattern:

```kotlin
@Composable
private fun VideoPlayer(
    item: MediaItem,
    isVisible: Boolean,
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(item.uri))
            prepare()
            playWhenReady = isVisible
        }
    }

    DisposableEffect(isVisible) {
        player.playWhenReady = isVisible
        onDispose {
            player.playWhenReady = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            androidx.media3.ui.PlayerView(ctx).apply {
                this.player = player
                useController = true
                setShowNextButton(false)
                setShowPreviousButton(false}
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
```

Note: There's a syntax error in the factory lambda above — the `setShowPreviousButton(false)}` should be `setShowPreviousButton(false)` (no closing brace after the paren). Use the corrected version:

```kotlin
@Composable
private fun VideoPlayer(
    item: MediaItem,
    isVisible: Boolean,
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(item.uri))
            prepare()
            playWhenReady = isVisible
        }
    }

    DisposableEffect(isVisible) {
        player.playWhenReady = isVisible
        onDispose {
            player.playWhenReady = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            androidx.media3.ui.PlayerView(ctx).apply {
                this.player = player
                useController = true
                setShowNextButton(false)
                setShowPreviousButton(false)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
```

**Verify**:
```bash
grep -n 'MediaType.VIDEO\|VideoPlayer\|ExoPlayer\|PlayerView\|AndroidView' app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt
```
→ Shows video type check, VideoPlayer composable, ExoPlayer setup, PlayerView usage.

Verify no `import` collision on `MediaItem`:
```bash
grep -n 'import.*MediaItem' app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt
```
→ Shows both `import io.github.kardeiro.gallery.data.model.MediaItem` and
  `import androidx.media3.common.MediaItem as Media3Item`. The `as Media3Item`
  alias prevents the name collision.

### Step 6: Build and verify

```bash
gradle assembleDebug
```
→ `BUILD SUCCESSFUL`

If gradle is not available, run the static check:
```bash
echo "=== Video support indicators ==="
echo "MediaRepository: queryMedia references:"
grep -c 'queryMedia' app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt
echo "GalleryScreen: MediaType.VIDEO references:"
grep -c 'MediaType.VIDEO' app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt
echo "GalleryScreen: formatDuration defined:"
grep -c 'formatDuration' app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt
echo "ViewerScreen: VideoPlayer references:"
grep -c 'VideoPlayer' app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt
echo "ViewerScreen: ExoPlayer references:"
grep -c 'ExoPlayer' app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt
echo "=== Dependency entries ==="
grep -E 'media3' gradle/libs.versions.toml app/build.gradle.kts
echo "=== Out of scope check ==="
git status --short
```
Expected: all counts > 0, no unexpected modified files.

## Test plan

No new tests — this is a new feature. Manual verification on a device with
both photos and videos:
1. Open app → gallery grid shows both photo and video thumbnails
2. Video thumbnails show a duration badge (e.g. "0:15") in bottom-right corner
3. Tap a video thumbnail → Viewer opens with video playback controls
4. Play/pause, seek, and rotate work correctly
5. Tap a photo thumbnail → ZoomableImage works as before (no regression)

Manual verification is outside scope for the executor; note it for the
human reviewer.

## Done criteria

ALL must hold:

- [ ] `gradle assembleDebug` exits 0 (or static check passes all grep counts)
- [ ] `grep -c 'Video.Media' app/src/main/java/.../MediaRepository.kt` > 0
- [ ] `grep -c 'MediaType.VIDEO' app/src/main/java/.../GalleryScreen.kt` > 0
- [ ] `grep -c 'formatDuration' app/src/main/java/.../GalleryScreen.kt` > 0
- [ ] `grep -c 'ExoPlayer' app/src/main/java/.../ViewerScreen.kt` > 0
- [ ] `grep -c 'PlayerView' app/src/main/java/.../ViewerScreen.kt` > 0
- [ ] No files outside the in-scope list are modified (`git status --short`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- The code at the locations in "Current state" doesn't match the excerpts
  (the codebase has drifted since this plan was written).
- A step's verification fails twice after a reasonable fix attempt.
- The fix appears to require touching an out-of-scope file.
- The Media3 library version `1.5.1` cannot be resolved at build time
  (use the latest 1.x available — update the version in
  `gradle/libs.versions.toml` and note the change).
- `MediaItem` import collides with `androidx.media3.common.MediaItem` —
  the plan uses `as Media3Item` to alias it; if that fails, use a
  different alias like `AndroidxMediaItem`.

## Maintenance notes

- The video query in `MediaRepository` loads ALL videos from the device.
  Future pagination work must account for both image and video queries.
- The `VideoPlayer` composable creates a new `ExoPlayer` per video item.
  For production, consider a single shared player with media item switching
  to avoid resource overhead. Deferred to keep this plan simple.
- `formatDuration` in `GalleryScreen.kt` is a utility that may be useful
  in other screens later — extract to a shared file when a second consumer
  appears.
- Video playback in albums (`AlbumDetailScreen`) is intentionally deferred;
  add it when album video support is planned.
