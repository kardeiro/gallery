# Plan 001: Fix dead AlbumView route — register missing composable

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 17fad43..HEAD -- app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt`
> If `NavGraph.kt` or `AlbumScreen.kt` changed since this plan was written,
> compare the "Current state" excerpts against the live code before proceeding;
> on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: XS
- **Risk**: LOW
- **Depends on**: none
- **Category**: bug
- **Planned at**: commit `17fad43`, 2026-06-21

## Why this matters

The app declares an `ALBUM_VIEW` route (`album/{bucketId}/{bucketName}`) and
`AlbumScreen` navigates to it when the user taps an album card. But the route
was never registered as a `composable()` inside `NavHost` — so every tap on an
album card crashes the app with an `IllegalArgumentException` ("Navigation
destination that matches request cannot be found"). This is the #1 crash
blocker: the app is effectively unusable beyond the gallery grid because no
album can be opened.

## Current state

**`app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt`**

The `Routes` object defines `ALBUM_VIEW` at line 17:
```kotlin
const val ALBUM_VIEW = "album/{bucketId}/{bucketName}"
```

And the `NavHost` navigates to this route at line 45 (inside the `composable(Routes.ALBUMS)` block):
```kotlin
AlbumScreen(
    onNavigateToAlbum = { bucketId, bucketName ->
        navController.navigate(Routes.albumRoute(bucketId, bucketName))
    },
    onBack = { navController.popBackStack() }
)
```

But **no** `composable(Routes.ALBUM_VIEW) { ... }` call exists anywhere in the
`NavHost` — the route has no destination. This is the bug.

**`app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt`**

The `AlbumScreen` already has the correct signature:
```kotlin
fun AlbumScreen(
    onNavigateToAlbum: (String, String) -> Unit,
    onBack: () -> Unit,
)
```

There is currently **no** `AlbumDetailScreen` or `AlbumViewScreen` composable
that accepts `bucketId` + `bucketName` to display the contents of a single
album. This plan creates one and wires it to the route.

**Repository conventions (match these):**
- File naming: `Screen.kt` suffix for screen-level composables in `ui/screen/`
- Function style: top-level `@Composable fun` with named parameters, `@OptIn(ExperimentalMaterial3Api::class)` where needed
- Navigation args: `navArgument(...) { type = NavType.StringType }` for string params
- Theme: uses `GalleryTheme` wrapper (already applied at `MainActivity.kt` level)

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Build APK | `gradle assembleDebug` | `BUILD SUCCESSFUL`, exit 0 |

## Scope

**In scope** (the only files you should modify):
- `app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt` (already exists — add `AlbumDetailScreen` at the end of the same file OR create a separate file under `ui/screen/`; see decision in Step 1)

**Out of scope** (do NOT touch, even though they look related):
- `GalleryScreen.kt`, `ViewerScreen.kt` — unrelated screens
- `MediaRepository.kt`, `MediaItem.kt` — no data model changes needed
- The `GalleryScreen` bottom-bar "Albums" tab navigation — that path works correctly

## Git workflow

- Branch: `advisor/001-fix-album-view-route`
- Commit per step; message style: conventional commits (`git log --oneline` shows no convention yet, so use: `fix: register ALBUM_VIEW route` and then `feat: add AlbumDetailScreen composable`)
- Do NOT push or open a PR

## Steps

### Step 1: Add `AlbumDetailScreen` composable

Create a new file `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt`.

This screen receives a `bucketId` and `bucketDisplayName`, loads media items
filtered to that bucket, and displays them in a grid identical to the main
`GalleryScreen` grid but with a back-navigating top bar and the album name as
title.

The implementation pattern to follow is the existing `AlbumScreen.kt` for the
top-bar layout and `GalleryScreen.kt` for the grid layout. Specifically:

**File: `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt`**

```kotlin
package io.github.kardeiro.gallery.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.kardeiro.gallery.data.MediaRepository
import io.github.kardeiro.gallery.data.model.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    bucketId: String,
    bucketDisplayName: String,
    onBack: () -> Unit,
    onNavigateToViewer: (Int) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { MediaRepository(context) }
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    LaunchedEffect(bucketId) {
        mediaItems = repository.loadMedia().filter { it.bucketId == bucketId }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(bucketDisplayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(
                items = mediaItems,
                key = { it.id }
            ) { item ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.small)
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
                }
            }
        }
    }
}
```

The pattern above follows `AlbumScreen.kt`'s top-bar approach and
`GalleryScreen.kt`'s grid approach exactly. The key difference is:
- Accepts `bucketId` + `bucketDisplayName` as parameters
- Filters `loadMedia()` by `bucketId`
- No bottom bar (it's a sub-screen)
- No permission/loading/empty state handling (the album screen is only reachable after the main gallery has already loaded data)

**Verify**:
```bash
ls app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt
```
→ shows the file exists.

### Step 2: Register the `ALBUM_VIEW` composable in `NavGraph.kt`

Open `app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt`.

Add an import for `AlbumDetailScreen` alongside the existing screen imports:
```kotlin
import io.github.kardeiro.gallery.ui.screen.AlbumDetailScreen
```

Then add a new `composable()` block inside `NavHost`, right after the existing
`composable(Routes.ALBUMS)` block (between lines 49 and 51). The route uses
two string arguments (`bucketId` and `bucketName`):

```kotlin
composable(
    route = Routes.ALBUM_VIEW,
    arguments = listOf(
        navArgument("bucketId") { type = NavType.StringType },
        navArgument("bucketName") { type = NavType.StringType },
    )
) { backStackEntry ->
    val bucketId = backStackEntry.arguments?.getString("bucketId") ?: return@composable
    val bucketName = backStackEntry.arguments?.getString("bucketName") ?: return@composable
    AlbumDetailScreen(
        bucketId = bucketId,
        bucketDisplayName = bucketName,
        onBack = { navController.popBackStack() },
        onNavigateToViewer = { index ->
            navController.navigate(Routes.viewerRoute(index))
        }
    )
}
```

After the edit, the `NavHost` block should contain four `composable()` calls:
1. `composable(Routes.GALLERY)`
2. `composable(Routes.ALBUMS)`
3. `composable(route = Routes.ALBUM_VIEW, ...)` ← NEW
4. `composable(route = Routes.VIEWER, ...)`

**Verify**:
```bash
grep -n "ALBUM_VIEW\|AlbumDetailScreen" app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt
```
→ Shows exactly one `import AlbumDetailScreen`, one `composable(route = Routes.ALBUM_VIEW`, and the existing `const val ALBUM_VIEW` + `fun albumRoute`.

### Step 3: Build and verify

Build the debug APK to confirm compilation succeeds:

```bash
gradle assembleDebug
```

Expected output ends with `BUILD SUCCESSFUL in X`s.

> **Note**: If you see a "Unresolved reference" error about `AlbumDetailScreen`,
> the import path in Step 2 doesn't match the actual file package. Fix the
> import to match what you wrote in Step 1.

## Test plan

There is no existing test framework in this project. For now, the verification
is the build succeeding (`gradle assembleDebug`). In a future plan,
instrumentation tests can be added; for this fix, manual verification by
tapping an album card in the running app and confirming it navigates to the
album's contents without crashing is sufficient.

## Done criteria

ALL must hold:

- [ ] `gradle assembleDebug` exits 0
- [ ] New file `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt` exists and compiles
- [ ] `NavGraph.kt` contains exactly one `composable(route = Routes.ALBUM_VIEW,` block
- [ ] No files outside the in-scope list are modified (`git status` shows only `NavGraph.kt` and `AlbumDetailScreen.kt` as changed/new)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- The code at the locations in "Current state" doesn't match the excerpts (the codebase has drifted since this plan was written).
- A step's verification fails twice after a reasonable fix attempt.
- The fix appears to require touching an out-of-scope file.
- You discover that `loadMedia()` does not actually return `bucketId` values matching the `Album.bucketId` — verify by checking `MediaRepository.kt:48` uses `MediaStore.Images.Media.BUCKET_ID` column in both `loadMedia()` and `loadAlbums()` projection.

## Maintenance notes

- If `loadMedia()` is later changed to also load videos, the album detail filter
  on `bucketId` still works correctly because both images and videos in the same
  album share the same bucket ID.
- If navigation is later refactored to use a sealed class / type-safe navigation
  approach (e.g. with Kotlin Serialization), this route must be updated to
  match the new pattern.
- The `AlbumDetailScreen` currently includes no loading/empty state because it
  reuses the already-loaded data. If a future plan introduces ViewModels with
  separate per-album loading, those states should be added.
