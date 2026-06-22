# Plan 016: Share one MediaRepository instance across screens

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report; do not improvise. When done, update the status row for this plan in `plans/README.md` unless a reviewer tells you they maintain the index.
>
> **Drift check (run first)**: `git diff --stat 8b40871..HEAD -- app/src/main/java/io/github/kardeiro/gallery/MainActivity.kt app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt`
> If any in-scope file changed since this plan was written, compare the "Current state" excerpts against the live code before proceeding; on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: MED
- **Depends on**: none
- **Category**: perf
- **Planned at**: commit `8b40871`, 2026-06-22

## Why this matters

`MediaRepository` has an in-memory `cachedMedia` list, but every screen creates its own repository instance. That makes the cache mostly route-local: opening albums, an album detail, or the viewer can re-query `MediaStore` even when the gallery screen already loaded the same data. A gallery app's largest hot path is scanning device media, so sharing one repository instance gives the existing cache a real app-wide lifetime without introducing a database or a new architecture.

## Current state

- `app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt` owns the media cache.
- `app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt` wires all screens.
- `GalleryScreen.kt`, `AlbumScreen.kt`, `AlbumDetailScreen.kt`, and `ViewerScreen.kt` each construct a repository with the current `Context`.

Relevant excerpts:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt:10-20
class MediaRepository(private val context: Context) {

    private var cachedMedia: List<MediaItem>? = null

    fun loadMedia(): List<MediaItem> {
        if (cachedMedia != null) return cachedMedia!!

        val images = queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)
        val videos = queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DURATION)
        return (images + videos).sortedByDescending { it.dateTaken }
            .also { cachedMedia = it }
    }
```

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt:66-68
val context = LocalContext.current
val repository = remember { MediaRepository(context) }
var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

// app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt:53-55
val context = LocalContext.current
val repository = remember { MediaRepository(context) }
var albums by remember { mutableStateOf<List<Album>>(emptyList()) }

// app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt:50-52
val context = LocalContext.current
val repository = remember { MediaRepository(context) }
var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

// app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt:67-69
val context = LocalContext.current
val repository = remember { MediaRepository(context) }
var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
```

Repo conventions to match:

- Screens are plain composable functions under `ui/screen` and receive navigation callbacks as parameters.
- The navigation graph in `NavGraph.kt` already passes screen dependencies and callbacks explicitly instead of using a DI framework.
- Keep the app minimal; do not add Hilt/Koin or a ViewModel layer for this plan.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Build | `./gradlew assembleDebug` | exit 0 and `BUILD SUCCESSFUL` |
| Status | `git status --short` | only intended files changed |

## Scope

**In scope**:

- `app/src/main/java/io/github/kardeiro/gallery/MainActivity.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`
- `app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt` only if needed to store `applicationContext`
- `plans/README.md`

**Out of scope**:

- Adding a dependency injection framework.
- Introducing ViewModels or persistent storage.
- Changing navigation routes or screen UI.
- Changing the repository query behavior beyond making the existing cache shared.

## Git workflow

- Work on the existing `autonomous-improve` branch.
- Follow the repo's existing commit style if committing, for example `autonomous-improve: share MediaRepository cache - loop #<N>`.
- Do not push unless the operator explicitly instructed you to push.

## Steps

### Step 1: Make MediaRepository safe to keep for the composition lifetime

In `MediaRepository`, store the application context instead of the activity context:

```kotlin
class MediaRepository(context: Context) {
    private val appContext = context.applicationContext
```

Then replace `context.contentResolver` with `appContext.contentResolver` in repository methods.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 2: Create the repository once near NavGraph

Update `NavGraph` to create one remembered repository using `LocalContext.current`:

```kotlin
val context = LocalContext.current
val repository = remember { MediaRepository(context) }
```

Import `androidx.compose.runtime.remember`, `androidx.compose.ui.platform.LocalContext`, and `io.github.kardeiro.gallery.data.MediaRepository` as needed.

Pass `repository` into `GalleryScreen`, `AlbumScreen`, `AlbumDetailScreen`, and `ViewerScreen`.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 3: Accept repository as a screen parameter

For each screen, add a `repository: MediaRepository` parameter and remove the local `LocalContext`/`remember { MediaRepository(context) }` repository construction. Keep `LocalContext.current` only in composables that still need it for Coil or sharing.

Expected shape:

```kotlin
fun GalleryScreen(
    repository: MediaRepository,
    onNavigateToAlbums: () -> Unit,
    onNavigateToViewer: (Int) -> Unit,
) {
```

Repeat for `AlbumScreen`, `AlbumDetailScreen`, and `ViewerScreen`.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 4: Update the plan index

In `plans/README.md`, change plan 016 status from `TODO` to `DONE ✅` after the build passes.

**Verify**: `git status --short` -> only the in-scope files are modified.

## Test plan

- No automated tests exist in this repo today.
- Manual smoke test after installing the debug APK: grant media permissions, open the gallery, switch to Albums, open an album, open the viewer, go back and repeat. The UI should show the same media while avoiding repeated full `MediaStore` scans after the first load.
- Verification command: `./gradlew assembleDebug` -> `BUILD SUCCESSFUL`.

## Done criteria

- [ ] `./gradlew assembleDebug` exits 0 and prints `BUILD SUCCESSFUL`.
- [ ] `MediaRepository(` is constructed in `NavGraph.kt` or one app-level location, not inside each screen.
- [ ] Each screen receives the shared `MediaRepository` through a parameter.
- [ ] `MediaRepository` uses `applicationContext` internally.
- [ ] No files outside the in-scope list are modified.
- [ ] `plans/README.md` marks plan 016 as `DONE ✅`.

## STOP conditions

Stop and report back if:

- The screen signatures or navigation graph have drifted from the excerpts above.
- The change requires adding a DI framework, ViewModel, or persistent cache.
- The build fails twice after reasonable import/signature fixes.
- Sharing the repository exposes stale media after delete and cannot be fixed by the existing `invalidateCache()` or `deleteMedia()` behavior.

## Maintenance notes

- If a ViewModel or DI layer is introduced later, keep one repository/cache owner per app/session rather than returning to per-screen construction.
- Reviewers should confirm no activity context is retained by a long-lived repository.
- This plan intentionally keeps the cache in memory only; persistence and content-observer invalidation are separate future work.
