# Plan 017: Move MediaStore queries off the main thread

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report; do not improvise. When done, update the status row for this plan in `plans/README.md` unless a reviewer tells you they maintain the index.
>
> **Drift check (run first)**: `git diff --stat 8b40871..HEAD -- app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt app/build.gradle.kts gradle/libs.versions.toml`
> If any in-scope file changed since this plan was written, compare the "Current state" excerpts against the live code before proceeding; on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: MED
- **Depends on**: plans/016-share-mediarepository-instance.md
- **Category**: perf
- **Planned at**: commit `8b40871`, 2026-06-22

## Why this matters

The app queries `MediaStore` from `LaunchedEffect` and from a permission callback. `LaunchedEffect` runs on the main dispatcher unless work is explicitly moved, and `ContentResolver.query()` can block on large media libraries or slow storage. Moving repository loads to `Dispatchers.IO` keeps Compose responsive during initial gallery load, album load, viewer open, and post-delete refresh.

## Current state

Repository methods are synchronous:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt:14-21
fun loadMedia(): List<MediaItem> {
    if (cachedMedia != null) return cachedMedia!!

    val images = queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)
    val videos = queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DURATION)
    return (images + videos).sortedByDescending { it.dateTaken }
        .also { cachedMedia = it }
}
```

Call sites run the synchronous work directly:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt:95-114
LaunchedEffect(Unit) {
    hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ...
    }
    if (hasPermission) {
        mediaItems = repository.loadMedia()
        isLoading = false
    } else {
        isLoading = false
    }
}

// app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt:57-59
LaunchedEffect(Unit) {
    albums = repository.loadAlbums()
}

// app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt:57-59
LaunchedEffect(bucketId) {
    mediaItems = repository.loadMedia().filter { it.bucketId == bucketId }
}

// app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt:76-79
LaunchedEffect(Unit) {
    val all = repository.loadMedia()
    mediaItems = if (bucketId != null) all.filter { it.bucketId == bucketId } else all
}
```

Repo conventions to match:

- This app already uses Kotlin coroutines transitively through AndroidX lifecycle/runtime and Compose.
- Keep repository APIs simple; do not introduce Flow, paging, or a database in this plan.
- If plan 016 has landed, use the shared repository instance rather than adding per-screen workarounds.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Build | `./gradlew assembleDebug` | exit 0 and `BUILD SUCCESSFUL` |
| Status | `git status --short` | only intended files changed |

## Scope

**In scope**:

- `app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`
- `app/build.gradle.kts` and `gradle/libs.versions.toml` only if `kotlinx-coroutines-android` is not already available to compile imports
- `plans/README.md`

**Out of scope**:

- Paging large media libraries.
- Adding database persistence.
- Adding content observers or automatic refresh.
- Changing UI layout, permissions, or routes.

## Git workflow

- Work on the existing `autonomous-improve` branch.
- Follow the repo's existing commit style if committing, for example `autonomous-improve: move media queries to IO dispatcher - loop #<N>`.
- Do not push unless the operator explicitly instructed you to push.

## Steps

### Step 1: Move repository query bodies to Dispatchers.IO

In `MediaRepository.kt`, import `kotlinx.coroutines.Dispatchers` and `kotlinx.coroutines.withContext`.

Change `loadMedia()` and `loadAlbums()` to `suspend` functions and wrap query work in `withContext(Dispatchers.IO)`. Preserve the fast cached return path.

Target shape:

```kotlin
suspend fun loadMedia(): List<MediaItem> {
    cachedMedia?.let { return it }
    return withContext(Dispatchers.IO) {
        cachedMedia?.let { return@withContext it }
        val images = queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)
        val videos = queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DURATION)
        (images + videos).sortedByDescending { it.dateTaken }
            .also { cachedMedia = it }
    }
}
```

Apply the same pattern to `loadAlbums()`: use the cached-media path when available, and run fallback `queryAlbums()` calls inside `withContext(Dispatchers.IO)`.

**Verify**: `./gradlew assembleDebug` -> if coroutine imports are unresolved, continue to Step 2; otherwise exits 0 and prints `BUILD SUCCESSFUL`.

### Step 2: Add coroutine dependency only if required

If the build fails because `kotlinx.coroutines.Dispatchers` or `withContext` is unresolved, add a version entry and library alias for `org.jetbrains.kotlinx:kotlinx-coroutines-android`, then add `implementation(...)` in `app/build.gradle.kts`.

Do not add this dependency if the build already succeeds through transitive dependencies.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 3: Update call sites for suspend repository methods

Existing `LaunchedEffect` blocks can call suspend functions directly, so keep those calls in place. The permission launcher callback in `GalleryScreen.kt` is not suspend. Wrap its reload in a remembered coroutine scope:

```kotlin
val scope = rememberCoroutineScope()

// inside RequestMultiplePermissions callback
if (allGranted) {
    scope.launch {
        isLoading = true
        mediaItems = repository.loadMedia()
        isLoading = false
    }
}
```

Import `androidx.compose.runtime.rememberCoroutineScope` and `kotlinx.coroutines.launch` as needed.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 4: Update the plan index

In `plans/README.md`, change plan 017 status from `TODO` to `DONE ✅` after the build passes.

**Verify**: `git status --short` -> only the in-scope files are modified.

## Test plan

- No automated tests exist in this repo today.
- Manual smoke test after installing the debug APK: grant permissions and confirm the gallery loads, albums load, album detail loads, viewer opens, and deleting an item refreshes without crashing.
- Verification command: `./gradlew assembleDebug` -> `BUILD SUCCESSFUL`.

## Done criteria

- [ ] `./gradlew assembleDebug` exits 0 and prints `BUILD SUCCESSFUL`.
- [ ] `MediaRepository.loadMedia()` is `suspend` and runs uncached `MediaStore` query work on `Dispatchers.IO`.
- [ ] `MediaRepository.loadAlbums()` is `suspend` and runs fallback `MediaStore` album query work on `Dispatchers.IO`.
- [ ] The permission callback in `GalleryScreen.kt` launches a coroutine instead of calling a suspend function directly.
- [ ] No paging/database/UI route changes are included.
- [ ] `plans/README.md` marks plan 017 as `DONE ✅`.

## STOP conditions

Stop and report back if:

- Plan 016 has not landed and the repository is still constructed independently in each screen.
- The build requires broad dependency or Gradle restructuring beyond adding `kotlinx-coroutines-android`.
- The coroutine change causes stale-cache races that cannot be resolved with a simple double-check inside `withContext`.
- The build fails twice after reasonable import/signature fixes.

## Maintenance notes

- If future work adds a `ContentObserver`, keep invalidation thread-safe because `cachedMedia` may be read on main and updated on IO.
- Reviewers should scrutinize loading state transitions in `GalleryScreen`; `isLoading` should always return to false after permission-granted reload attempts.
- Paging large libraries is intentionally deferred; this plan removes main-thread blocking but still loads all media into memory.
