# Plan 019: Defer ExoPlayer creation to the visible video page

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report; do not improvise. When done, update the status row for this plan in `plans/README.md` unless a reviewer tells you they maintain the index.
>
> **Drift check (run first)**: `git diff --stat 8b40871..HEAD -- app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`
> If the in-scope file changed since this plan was written, compare the "Current state" excerpts against the live code before proceeding; on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: MED
- **Depends on**: none
- **Category**: perf
- **Planned at**: commit `8b40871`, 2026-06-22

## Why this matters

The viewer creates and prepares an `ExoPlayer` inside each composed video page. `HorizontalPager` may compose pages around the current page during gestures or state changes, so a non-visible video page can allocate a player and call `prepare()` before the user actually views it. Deferring player creation until the page is visible reduces memory pressure and avoids decoder work for videos the user may only swipe past.

## Current state

The pager sends every video page to `VideoPlayer`, with visibility only controlling `playWhenReady`:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt:152-164
HorizontalPager(
    state = pagerState,
    modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
) { page ->
    val item = mediaItems[page]
    if (item.mediaType == MediaType.VIDEO) {
        VideoPlayer(
            item = item,
            isVisible = page == pagerState.currentPage,
            onToggleBars = { showBars = !showBars }
        )
```

`VideoPlayer` creates and prepares the player immediately when composed:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt:226-239
@Composable
private fun VideoPlayer(
    item: MediaItem,
    isVisible: Boolean,
    onToggleBars: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(item.uri))
            prepare()
            playWhenReady = isVisible
        }
    }
```

Repo conventions to match:

- Keep video playback local to `ViewerScreen.kt`; no new player manager class is needed.
- Use Compose lifecycle primitives already present in the file (`remember`, `DisposableEffect`).
- Preserve tap-to-toggle bars and existing `PlayerView` controller behavior.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Build | `./gradlew assembleDebug` | exit 0 and `BUILD SUCCESSFUL` |
| Status | `git status --short` | only intended files changed |

## Scope

**In scope**:

- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`
- `plans/README.md`

**Out of scope**:

- Replacing Media3 or `PlayerView`.
- Adding video thumbnails or preloading.
- Changing pager navigation, image zoom, sharing, info, or delete behavior.

## Git workflow

- Work on the existing `autonomous-improve` branch.
- Follow the repo's existing commit style if committing, for example `autonomous-improve: defer ExoPlayer setup - loop #<N>`.
- Do not push unless the operator explicitly instructed you to push.

## Steps

### Step 1: Avoid composing PlayerView for non-visible video pages

In the `HorizontalPager` video branch, keep the `VideoPlayer` call only for the visible page. For non-visible video pages, render a lightweight placeholder `Box` that fills the available size and still supports tap-to-toggle if needed.

Target shape:

```kotlin
if (item.mediaType == MediaType.VIDEO) {
    if (page == pagerState.currentPage) {
        VideoPlayer(
            item = item,
            onToggleBars = { showBars = !showBars }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize())
    }
} else {
```

After this change, remove the `isVisible` parameter from `VideoPlayer`.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 2: Key player creation by media URI

Update `VideoPlayer` so `remember` is keyed by `item.uri`:

```kotlin
val player = remember(item.uri) {
    ExoPlayer.Builder(context).build().apply {
        setMediaItem(Media3Item.fromUri(item.uri))
        prepare()
        playWhenReady = true
    }
}
```

Keep the `DisposableEffect(Unit)` that releases the player on disposal, or key it by `player` if needed after refactoring.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 3: Remove now-unnecessary visibility side effect

Remove the `DisposableEffect(isVisible)` block that only toggles `playWhenReady`. Since `VideoPlayer` is composed only for the visible page, the player can start with `playWhenReady = true` and release when the page leaves composition.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 4: Update the plan index

In `plans/README.md`, change plan 019 status from `TODO` to `DONE ✅` after the build passes.

**Verify**: `git status --short` -> only `ViewerScreen.kt` and `plans/README.md` are modified.

## Test plan

- No automated tests exist in this repo today.
- Manual smoke test with at least two videos and one image: open a video, swipe to another item, swipe back, and verify playback starts only on the visible video page. Confirm tapping still toggles the top bar and the controller still appears.
- Verification command: `./gradlew assembleDebug` -> `BUILD SUCCESSFUL`.

## Done criteria

- [ ] `./gradlew assembleDebug` exits 0 and prints `BUILD SUCCESSFUL`.
- [ ] `VideoPlayer` no longer accepts `isVisible`.
- [ ] `ExoPlayer.Builder(...).build()` is reachable only when the video page is the current visible page.
- [ ] Player creation is keyed by `item.uri` and the player is still released in `DisposableEffect`.
- [ ] No files outside `ViewerScreen.kt` and `plans/README.md` are modified.
- [ ] `plans/README.md` marks plan 019 as `DONE ✅`.

## STOP conditions

Stop and report back if:

- The pager has already been refactored to preload videos intentionally.
- Removing the visibility side effect breaks playback lifecycle in a way that requires a new player manager or app-wide playback state.
- The build fails twice after reasonable signature/import fixes.

## Maintenance notes

- This plan optimizes memory and decoder work by not preparing off-screen videos. It may slightly delay video start after a swipe; if that becomes noticeable, consider a later explicit preloading design with a bounded player pool.
- Reviewers should check for player release paths carefully because leaked `ExoPlayer` instances are expensive on Android.
