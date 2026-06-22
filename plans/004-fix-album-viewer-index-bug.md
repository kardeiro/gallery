# Plan 004: Fix wrong item shown when navigating from album to viewer

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. Your reviewer maintains the index; skip updating
> `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 107fab5..HEAD -- app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: bug
- **Planned at**: commit `107fab5`, 2026-06-22

## Why this matters

When the user taps a photo inside an album (AlbumDetailScreen), the app
navigates to the full-screen viewer (ViewerScreen). AlbumDetailScreen
computes the tapped item's index within the **filtered album list** and
passes it to the viewer. But ViewerScreen loads the **full unfiltered media
list** from the repository. The index is wrong — the viewer shows a
different item, or crashes if the index exceeds the full list's bounds.

This makes the album → viewer navigation unusable: no album photo opens
the correct item.

## Current state

**AlbumDetailScreen.kt:95** — passes index from filtered list:
```kotlin
.clickable { onNavigateToViewer(mediaItems.indexOf(item)) }
```

Where `mediaItems` at line 52 is:
```kotlin
var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
```

And loaded at line 54-56:
```kotlin
LaunchedEffect(bucketId) {
    mediaItems = repository.loadMedia().filter { it.bucketId == bucketId }
}
```

**ViewerScreen.kt:69-71** — loads the FULL unfiltered list:
```kotlin
LaunchedEffect(Unit) {
    mediaItems = repository.loadMedia()
}
```

**ViewerScreen.kt:73-76** — uses `initialIndex` with the full list:
```kotlin
val pagerState = rememberPagerState(
    initialPage = initialIndex,
    pageCount = { mediaItems.size }
)
```

**NavGraph.kt:21** — viewer route only accepts index:
```kotlin
fun viewerRoute(index: Int) = "viewer/$index"
```

**NavGraph.kt:72-73** — only `index` argument:
```kotlin
route = Routes.VIEWER,
arguments = listOf(navArgument("index") { type = NavType.IntType })
```

**GalleryScreen.kt:200-202** — passes index from the full list (this is correct):
```kotlin
val index = mediaItems.indexOf(item)
onNavigateToViewer(index)
```

### Repo conventions (match these)

- Navigation routes: defined as `const val` in `Routes` object, with helper functions for parameterized routes
- Nav arguments: `navArgument(name) { type = NavType.XxxType }` for each param
- Optional route params: use `?` query syntax (`route/{mandatory}?optional={optional}`) with `defaultValue` in `navArgument`
- Imports: `android.*`, then `androidx.*`, then `coil.*`, then `io.github.kardeiro.*`
- Compose function style: top-level `@Composable fun`, `@OptIn(ExperimentalMaterial3Api::class)` where needed
- Screen private composables at bottom of file

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Build APK | `gradle assembleDebug` | `BUILD SUCCESSFUL`, exit 0 |

Note: if `gradle` is not available in the worktree, run a static check using the grep commands in each step's Verify section.

## Scope

**In scope** (the only files you should modify):
- `app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt` — add optional `bucketId` to VIEWER route
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt` — pass `bucketId` when navigating to viewer
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt` — accept `bucketId`, filter when present

**Out of scope** (do NOT touch):
- `GalleryScreen.kt` — it passes index from the full list (already correct)
- `MediaRepository.kt`, `MediaItem.kt` — no data model changes needed
- `AlbumScreen.kt` — not related
- Theme files, strings.xml, build configs

## Steps

### Step 1: Add optional `bucketId` parameter to the VIEWER route

Open `app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt`.

**1a.** Update `Routes.VIEWER` to include an optional query parameter:
```kotlin
const val VIEWER = "viewer/{index}?bucketId={bucketId}"
```

**1b.** Update `viewerRoute()` to accept an optional `bucketId`:
```kotlin
fun viewerRoute(index: Int, bucketId: String? = null) =
    if (bucketId != null) "viewer/$index?bucketId=$bucketId" else "viewer/$index"
```

**1c.** Update the `composable(Routes.VIEWER, ...)` block to extract `bucketId`:
```kotlin
composable(
    route = Routes.VIEWER,
    arguments = listOf(
        navArgument("index") { type = NavType.IntType },
        navArgument("bucketId") { type = NavType.StringType; defaultValue = null },
    )
) { backStackEntry ->
    val index = backStackEntry.arguments?.getInt("index") ?: 0
    val bucketId = backStackEntry.arguments?.getString("bucketId")
    ViewerScreen(
        initialIndex = index,
        bucketId = bucketId,
        onBack = { navController.popBackStack() }
    )
}
```

**Verify**:
```bash
grep -n 'bucketId' app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt
```
→ Shows at least 4 references (const route, viewerRoute fun, navArgument, getString).

**Verify** the route string has both params:
```bash
grep 'VIEWER' app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt
```
→ Shows `const val VIEWER = "viewer/{index}?bucketId={bucketId}"`

### Step 2: Pass `bucketId` from AlbumDetailScreen

Open `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt`.

**2a.** Update the `onNavigateToViewer` call at line 95 to pass `bucketId`:
```kotlin
.clickable { onNavigateToViewer(mediaItems.indexOf(item)) }
```
→ change to:
```kotlin
.clickable {
    val index = mediaItems.indexOf(item)
    onNavigateToViewer(index)
}
```

Wait — `onNavigateToViewer` is called from `NavGraph.kt`, which currently calls:
```kotlin
onNavigateToViewer = { index ->
    navController.navigate(Routes.viewerRoute(index))
}
```

We need to update this lambda to pass `bucketId`. So in `NavGraph.kt`, change the `AlbumDetailScreen` composable invocation at line 64-67 from:
```kotlin
AlbumDetailScreen(
    bucketId = bucketId,
    bucketDisplayName = bucketName,
    onBack = { navController.popBackStack() },
    onNavigateToViewer = { index ->
        navController.navigate(Routes.viewerRoute(index))
    }
)
```
→ to:
```kotlin
AlbumDetailScreen(
    bucketId = bucketId,
    bucketDisplayName = bucketName,
    onBack = { navController.popBackStack() },
    onNavigateToViewer = { index ->
        navController.navigate(Routes.viewerRoute(index, bucketId))
    }
)
```

**Verify**:
```bash
grep -n 'viewerRoute' app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt
```
→ Shows one call with `(index, bucketId)` in the ALBUM_VIEW composable, and one with `(index)` in the GALLERY composable.

### Step 3: Accept `bucketId` in ViewerScreen and filter when present

Open `app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`.

**3a.** Change the function signature to accept `bucketId`:
```kotlin
fun ViewerScreen(
    initialIndex: Int,
    bucketId: String? = null,
    onBack: () -> Unit,
)
```

**3b.** Update the `LaunchedEffect(Unit)` block (line 69-71) to filter when `bucketId` is present:
```kotlin
LaunchedEffect(Unit) {
    mediaItems = repository.loadMedia()
}
```
→ change to:
```kotlin
LaunchedEffect(Unit) {
    mediaItems = let {
        val all = repository.loadMedia()
        if (bucketId != null) all.filter { it.bucketId == bucketId } else all
    }
}
```

Or equivalently:
```kotlin
LaunchedEffect(Unit) {
    val all = repository.loadMedia()
    mediaItems = if (bucketId != null) all.filter { it.bucketId == bucketId } else all
}
```

**Verify**:
```bash
grep -n 'bucketId' app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt
```
→ Shows 2 references (function parameter and filter condition).

### Step 4: Build and verify

```bash
gradle assembleDebug
```
Expected output ends with `BUILD SUCCESSFUL`.

If `gradle` is not available:
```bash
echo "=== Static verification ==="
echo "NavGraph: bucketId references:"
grep -c 'bucketId' app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt
echo "NavGraph: viewerRoute with bucketId:"
grep -c 'viewerRoute(index, bucketId)' app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt
echo "NavGraph: viewerRoute without bucketId:"
grep -c 'viewerRoute(index)' app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt
echo "ViewerScreen: bucketId references:"
grep -c 'bucketId' app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt
echo "Out of scope check:"
git status --short
```
Expected: all counts > 0, no unexpected modified files.

## Test plan

No new tests (no test framework exists). Manual verification:
1. Open app → tap "Albums" tab → tap any album
2. Tap a photo — viewer should show the same photo
3. Swipe left/right — should navigate within the album's photos only
4. Go back → tap a different photo → should show that photo
5. Go back to main gallery → tap a photo → viewer should show the full gallery (all media)

## Done criteria

ALL must hold:

- [ ] `gradle assembleDebug` exits 0 (or static check passes)
- [ ] `grep "viewerRoute(index, bucketId)" NavGraph.kt` shows that album detail passes bucketId
- [ ] `grep "viewerRoute(index)" NavGraph.kt | grep -v "bucketId"` exists for the gallery caller
- [ ] `grep "bucketId" ViewerScreen.kt` shows the filter in LaunchedEffect
- [ ] No files outside the in-scope list are modified (`git status --short`)

## STOP conditions

Stop and report back (do not improvise) if:

- The code at the locations in "Current state" doesn't match the excerpts
  (the codebase has drifted since this plan was written).
- A step's verification fails twice after a reasonable fix attempt.
- The fix appears to require touching an out-of-scope file.
- The route syntax `"viewer/{index}?bucketId={bucketId}"` causes a Navigation
  Compose runtime error — if so, try the form without braces around the
  optional param: `"viewer/{index}?bucketId={bucketId}"` is the standard
  Navigation Compose syntax for optional query params; this should work.

## Maintenance notes

- If a future plan adds a ViewModel, the bucketId filtering logic should move
  there so ViewerScreen doesn't need to know about it.
- GalleryScreen passes index from the full list (no bucketId), so it stays
  correct as-is.
- If the viewer is ever opened from another screen (e.g. search results),
  remember to pass the appropriate bucketId or omit it for full-list mode.
