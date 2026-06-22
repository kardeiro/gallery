# Plan 018: Remember Coil ImageRequest objects in media composables

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report; do not improvise. When done, update the status row for this plan in `plans/README.md` unless a reviewer tells you they maintain the index.
>
> **Drift check (run first)**: `git diff --stat 8b40871..HEAD -- app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`
> If any in-scope file changed since this plan was written, compare the "Current state" excerpts against the live code before proceeding; on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: perf
- **Planned at**: commit `8b40871`, 2026-06-22

## Why this matters

Dense grids and the viewer build new Coil `ImageRequest` objects directly during composition. Even when Coil can reuse cached bitmaps, allocating a new request object on every recomposition adds avoidable work and can make request identity less stable in scroll-heavy screens. Remembering requests by URI and requested size is a small, low-risk optimization that matches Compose expectations for stable models.

## Current state

The app builds requests inline in multiple composables:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt:224-228
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(item.uri)
        .size(360)
        .build(),

// app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt:123-128
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(album.coverUri)
        .size(360)
        .crossfade(true)
        .build(),

// app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt:100-104
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(item.uri)
        .size(360)
        .build(),

// app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt:206-211
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(item.uri)
        .size(2048)
        .crossfade(true)
        .build(),
```

Repo conventions to match:

- The app uses Coil's `AsyncImage` and `ImageRequest.Builder` directly.
- Existing screens prefer local composables over shared abstractions unless reuse is obvious.
- Do not add `remember` everywhere indiscriminately; limit it to image request construction keyed by URI and fixed request size.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Build | `./gradlew assembleDebug` | exit 0 and `BUILD SUCCESSFUL` |
| Status | `git status --short` | only intended files changed |

## Scope

**In scope**:

- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`
- `plans/README.md`

**Out of scope**:

- Changing thumbnail sizes.
- Replacing Coil.
- Adding a shared image component unless duplication remains small and exact.
- Changing visual styling or content descriptions.

## Git workflow

- Work on the existing `autonomous-improve` branch.
- Follow the repo's existing commit style if committing, for example `autonomous-improve: remember Coil image requests - loop #<N>`.
- Do not push unless the operator explicitly instructed you to push.

## Steps

### Step 1: Remember thumbnail requests in gallery grid

In `MediaThumbnail` in `GalleryScreen.kt`, keep `val context = LocalContext.current`, then create a remembered request before `AsyncImage`:

```kotlin
val imageRequest = remember(item.uri) {
    ImageRequest.Builder(context)
        .data(item.uri)
        .size(360)
        .build()
}
```

Pass `model = imageRequest` to `AsyncImage`. Add `androidx.compose.runtime.remember` only if it is not already imported.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 2: Remember album cover requests

In `AlbumCard` in `AlbumScreen.kt`, remember the cover request with `album.coverUri` as the key. Preserve the current `size(360)` and existing `crossfade(true)` behavior for this plan.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 3: Remember album-detail thumbnail requests

In the grid item body in `AlbumDetailScreen.kt`, create a remembered request keyed by `item.uri` and pass it to `AsyncImage`.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 4: Remember viewer image requests

In `ZoomableImage` in `ViewerScreen.kt`, remember the full-view request keyed by `item.uri`. Preserve `size(2048)` and `crossfade(true)` for this plan.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 5: Update the plan index

In `plans/README.md`, change plan 018 status from `TODO` to `DONE ✅` after the build passes.

**Verify**: `git status --short` -> only the in-scope files are modified.

## Test plan

- No automated tests exist in this repo today.
- Manual smoke test: scroll the gallery grid, open Albums, scroll album cards, open an album, and open the viewer. Images should still load at the same visual sizes.
- Verification command: `./gradlew assembleDebug` -> `BUILD SUCCESSFUL`.

## Done criteria

- [ ] `./gradlew assembleDebug` exits 0 and prints `BUILD SUCCESSFUL`.
- [ ] The four cited `AsyncImage` call sites pass remembered `ImageRequest` objects instead of constructing requests inline in the `model` argument.
- [ ] Request keys use the media URI or album cover URI.
- [ ] Existing request sizes and crossfade choices are preserved.
- [ ] No files outside the in-scope list are modified.
- [ ] `plans/README.md` marks plan 018 as `DONE ✅`.

## STOP conditions

Stop and report back if:

- The image-loading code has been refactored away from Coil or `AsyncImage`.
- Remembering the request requires changing public screen APIs or media models.
- The build fails twice after reasonable import fixes.

## Maintenance notes

- If future work introduces a shared thumbnail composable, move the remembered request into that composable and keep the key explicit.
- Reviewers should verify this plan does not accidentally change image decode sizes; that would be a separate performance/quality tradeoff.
