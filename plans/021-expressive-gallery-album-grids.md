# Plan 021: Redesign gallery and album grids with expressive media cards

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report; do not improvise. When done, update the status row for this plan in `plans/README.md` unless a reviewer tells you they maintain the index.
>
> **Drift check (run first)**: `git diff --stat 400d821..HEAD -- app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/theme app/src/main/res/values/strings.xml`
> If any in-scope file changed since this plan was written, compare the "Current state" excerpts against the live code before proceeding; on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: MED
- **Depends on**: plans/020-expressive-material3-theme-foundation.md
- **Category**: ui-ux
- **Planned at**: commit `400d821`, 2026-06-22

## Why this matters

The core gallery experience is currently a dense, edge-to-edge grid with 2dp gutters and simple clipped thumbnails. It is efficient, but it does not communicate Material You expressiveness or provide much hierarchy between top-level navigation, content, videos, albums, and empty/loading states. Redesigning the grid and album cards around shared M3 tokens can make the app feel more premium while preserving the content-first gallery purpose.

## Current state

Relevant files:

- `GalleryScreen.kt` — top-level photo/video grid, permission state, empty state, bottom navigation.
- `AlbumScreen.kt` — album grid and album cards.
- `AlbumDetailScreen.kt` — per-album grid.
- `strings.xml` — labels and text resources.
- `ui/theme/*` — Material theme and new expressive tokens from plan 020.

Gallery grid excerpt:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt:190-197
LazyVerticalGrid(
    state = gridState,
    columns = GridCells.Adaptive(120.dp),
    contentPadding = PaddingValues(2.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
    modifier = Modifier.fillMaxSize()
)
```

Thumbnail excerpt:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt:229-240
Box(
    modifier = Modifier
        .aspectRatio(1f)
        .clip(MaterialTheme.shapes.small)
        .background(MaterialTheme.colorScheme.surfaceVariant)
) {
    AsyncImage(
        model = imageRequest,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
```

Album card excerpt:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt:116-148
Card(
    onClick = onClick,
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
        ) {
```

Repo conventions to match:

- Keep screen code as Compose functions under `ui/screen`.
- Use `stringResource` for visible text; add strings to `strings.xml` when needed.
- Use Material icons already available through `material-icons-extended`; do not add icon libraries.
- Keep click handlers and navigation behavior unchanged.

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
- `app/src/main/java/io/github/kardeiro/gallery/ui/theme/*` only to use tokens created by plan 020, not to redefine them.
- `app/src/main/res/values/strings.xml` only for new visible labels.
- `plans/README.md`

**Out of scope**:

- Changing repository/data loading behavior.
- Changing navigation routes or viewer behavior.
- Adding search, selection mode, or sorting; those are product features, not this visual redesign.
- Replacing Coil or changing image decode sizes.

## Git workflow

- Work after plan 020 has landed.
- Commit message suggestion: `autonomous-improve: redesign media grids with expressive cards`.
- Do not push unless instructed.

## Steps

### Step 1: Create a reusable expressive thumbnail composable

In `GalleryScreen.kt`, evolve `MediaThumbnail` into a more expressive card-like tile while preserving its public inputs (`item`, `onClick`). Requirements:

- Add `.clickable(onClick = onClick)` or use a Material clickable container so tapping the thumbnail has a ripple/pressed state.
- Use a soft expressive corner radius from plan 020 tokens or `MaterialTheme.shapes.large`.
- Keep `aspectRatio(1f)` and `ContentScale.Crop`.
- Use `MaterialTheme.colorScheme.surfaceContainer` or `surfaceVariant` for the placeholder background.
- For videos, replace the tiny duration-only badge with a more legible pill: scrim background, a play icon, and duration text if duration exists.
- Ensure the visual badge has enough padding and remains readable in light/dark mode.

Do not change `onNavigateToViewer(idIndexMap[item.id] ?: 0)` behavior.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 2: Give the gallery grid expressive spacing and adaptive columns

In `GalleryScreen.kt`, update `LazyVerticalGrid` spacing:

- Use content padding around the grid (for example 8-12dp) instead of 2dp edge-to-edge.
- Use horizontal/vertical spacing around 6-8dp.
- Keep `GridCells.Adaptive`, but consider `112.dp` or `120.dp` minimum cell size so phones still show multiple columns.
- Ensure bottom content is not hidden behind `BottomAppBar` by keeping Scaffold inner padding applied.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 3: Redesign album cards as expressive stacked cards

In `AlbumScreen.kt`, update `AlbumCard`:

- Use `CardDefaults.elevatedCardColors` or `CardDefaults.cardColors` with `surfaceContainer`/`surfaceContainerHigh`.
- Use a larger corner radius consistent with plan 020.
- Add comfortable text padding: title and count should feel grouped but not cramped.
- Add `maxLines = 1` and `overflow = TextOverflow.Ellipsis` for long album names.
- Make the count visually secondary using `onSurfaceVariant`.
- Keep the existing `onClick` and album cover image behavior.

Add `import androidx.compose.ui.text.style.TextOverflow` if needed.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 4: Align album-detail grid with the gallery grid

In `AlbumDetailScreen.kt`, apply the same thumbnail spacing, shape, and clickable feedback as the top-level gallery grid. If practical without creating a new file, extract a small shared private thumbnail composable pattern inside one file only if it avoids duplication cleanly. Do not over-abstract.

Keep album-detail navigation behavior unchanged:

```kotlin
.clickable { onNavigateToViewer(idIndexMap[item.id] ?: 0) }
```

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 5: Update index

After the build passes, mark plan 021 as `DONE ✅` in `plans/README.md`.

**Verify**: `git status --short` -> only in-scope files are modified.

## Test plan

- Build: `./gradlew assembleDebug` -> `BUILD SUCCESSFUL`.
- Manual phone portrait: gallery grid should show multiple columns, clear video badges, and comfortable gutters.
- Manual small screen: no horizontal scrolling, no clipped album titles causing layout breakage.
- Manual dark mode: album cards, badges, text, and placeholders remain readable.
- Manual large font: album names and item counts do not overlap images.
- Manual tap test: tapping thumbnails and album cards still navigates to the correct viewer/album.

## Done criteria

- [ ] `./gradlew assembleDebug` exits 0 and prints `BUILD SUCCESSFUL`.
- [ ] Gallery and album-detail grids use expressive spacing greater than the current 2dp gutters.
- [ ] Media thumbnails have ripple/pressed feedback and expressive corners.
- [ ] Video items have a readable play/duration pill.
- [ ] Album cards use expressive Material 3 surfaces, corners, and text hierarchy.
- [ ] Navigation behavior is unchanged.
- [ ] `plans/README.md` marks plan 021 as `DONE ✅`.

## STOP conditions

Stop and report back if:

- Plan 020 tokens do not exist yet.
- A clean implementation requires changing data models or navigation signatures.
- The design requires adding a new dependency.
- The build fails twice after reasonable import/signature fixes.

## Maintenance notes

- Future selection/search features should reuse the expressive thumbnail container instead of creating a separate tile style.
- Reviewers should compare gallery and album-detail grids side by side to ensure spacing and interaction feel consistent.
- Keep performance in mind: avoid per-item animations that run continuously in the grid.
