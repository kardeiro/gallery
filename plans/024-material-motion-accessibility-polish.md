# Plan 024: Add Material motion polish and accessibility validation

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report; do not improvise. When done, update the status row for this plan in `plans/README.md` unless a reviewer tells you they maintain the index.
>
> **Drift check (run first)**: `git diff --stat 400d821..HEAD -- app/src/main/java/io/github/kardeiro/gallery/ui app/src/main/res/values/strings.xml`
> If any in-scope file changed since this plan was written, compare the "Current state" excerpts against the live code before proceeding; on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: MED
- **Depends on**: plans/020-expressive-material3-theme-foundation.md, plans/021-expressive-gallery-album-grids.md, plans/022-expressive-immersive-viewer-overlays.md, plans/023-expressive-states-feedback.md
- **Category**: ui-ux
- **Planned at**: commit `400d821`, 2026-06-22

## Why this matters

After the expressive visual redesign lands, the app needs a final pass for motion consistency, accessibility, and large-screen resilience. This is where many UI redesigns fail: individual screens look better, but tap targets, content descriptions, dynamic text, landscape layouts, and motion timing remain inconsistent. This plan turns the redesigned screens into a durable Material You experience rather than a cosmetic pass.

## Current state

Current accessibility and motion signals before the redesign:

- `GalleryScreen.kt` grid thumbnails use `contentDescription = null`, which is acceptable only if thumbnails are treated as decorative and the clickable parent has a meaningful semantic label.
- `AlbumScreen.kt` cover images use `contentDescription = album.displayName`, but the whole card click target should announce album name and item count.
- `ViewerScreen.kt` uses icon button content descriptions for back/share/info/delete, but overlay and dialog changes from plan 022 need revalidation.
- Navigation currently uses Compose Navigation without custom transitions.

Examples:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt:235-240
AsyncImage(
    model = imageRequest,
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = Modifier.fillMaxSize()
)
```

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt:129-133
AsyncImage(
    model = imageRequest,
    contentDescription = album.displayName,
    contentScale = ContentScale.Crop,
    modifier = Modifier.fillMaxSize()
)
```

Repo conventions to match:

- Keep changes in Compose UI files and string resources.
- Use Material/Compose APIs already available; do not add animation or accessibility dependencies.
- Prefer semantic modifiers and content descriptions over visual-only changes.
- Keep the app fast: no continuous animations in scrolling grids.

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
- `app/src/main/java/io/github/kardeiro/gallery/ui/navigation/NavGraph.kt` only if adding supported navigation transitions without new dependencies.
- `app/src/main/res/values/strings.xml`
- `plans/README.md`

**Out of scope**:

- New features such as search, selection, edit tools, or onboarding.
- Adding UI test frameworks.
- Adding third-party animation libraries.
- Reworking data/repository code.

## Git workflow

- Work only after plans 020-023 have landed.
- Commit message suggestion: `autonomous-improve: polish Material motion and accessibility`.
- Do not push unless instructed.

## Steps

### Step 1: Add semantic labels for media and album click targets

For clickable media thumbnails in `GalleryScreen.kt` and `AlbumDetailScreen.kt`:

- Add a semantic content description to the clickable container or use `Modifier.semantics` so screen readers can announce the item type and action.
- Use localized strings such as `Open photo`, `Open video`, or `Open media item`.
- Keep the `AsyncImage` content description null if the parent provides the meaningful clickable label.

For album cards in `AlbumScreen.kt`:

- Ensure the card announces album name and item count together.
- Avoid duplicate announcements from both image and card; if the card has full semantics, make the image decorative.

Add strings to `strings.xml` as needed.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 2: Confirm touch targets and spacing after redesign

Review all icon-only controls in the redesigned screens:

- Back/share/info/delete in viewer overlay.
- Back in album and album detail screens.
- Any new action buttons in state cards.

Ensure each target is at least 48dp through `IconButton`, `Button`, `Surface` plus padding, or explicit `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)`. Do not shrink icon buttons to fit visual styling.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 3: Add reduced-motion-safe animation policy

If plans 021-023 introduced animations, centralize durations in theme tokens or local constants and keep them between 150-300ms. Avoid infinite decorative animation.

If using `AnimatedVisibility`, ensure content remains accessible and no critical action is hidden with no alternative. There is no direct system reduced-motion API requirement in this repo today; do not add platform plumbing unless simple and dependency-free.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 4: Validate landscape and large-screen layout resilience

Make targeted layout fixes if the redesigned screens use overly narrow or overly wide content:

- State cards should use max-width-like constraints via `widthIn(max = ...)` while still filling phone width with padding.
- Dialog content should wrap rather than overflow.
- Grids should remain adaptive using `GridCells.Adaptive`.
- Viewer overlays should avoid screen edges with padding and not cover system gesture areas.

Do not introduce tablet-specific navigation in this plan; only fix resilience issues caused by the redesign.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 5: Add a manual UI QA checklist to the plan index

In `plans/README.md`, add a short `UI QA checklist` section covering:

- Light/dark mode.
- Large font size.
- TalkBack basic navigation.
- Small phone portrait.
- Landscape.
- At least one video item.
- Empty/no-permission state.

Then mark plan 024 as `DONE ✅` after build passes.

**Verify**: `git status --short` -> only in-scope files are modified.

## Test plan

- Build: `./gradlew assembleDebug` -> `BUILD SUCCESSFUL`.
- Manual TalkBack pass: navigate gallery grid, album cards, viewer actions, dialogs, and state cards; labels should be meaningful and not duplicated.
- Manual large-font pass: set Android display/font size high and verify no controls overlap.
- Manual dark-mode pass: verify contrast of overlays, cards, badges, snackbar/dialogs.
- Manual landscape pass: gallery grid and viewer overlays remain usable.
- Manual video pass: video player controls and custom overlays do not fight each other.

## Done criteria

- [ ] `./gradlew assembleDebug` exits 0 and prints `BUILD SUCCESSFUL`.
- [ ] Clickable media items and album cards have meaningful accessibility labels.
- [ ] All icon-only controls remain at least 48dp touch targets.
- [ ] Motion timing is consistent and no infinite decorative animations were added.
- [ ] State cards/dialogs/overlays tolerate large text and landscape.
- [ ] `plans/README.md` includes the UI QA checklist and marks plan 024 as `DONE ✅`.

## STOP conditions

Stop and report back if:

- Plans 020-023 have not landed, because this plan depends on their final UI shape.
- Accessibility fixes require changing navigation or data models.
- Compose APIs needed for semantics or animation are unavailable in the current dependency set.
- Build fails twice after reasonable import/signature fixes.

## Maintenance notes

- Re-run this polish pass after any future UI-heavy feature such as search, selection, or editing.
- Keep accessibility strings generic enough for localization but specific enough to be useful in TalkBack.
- Do not trade touch target size for visual density; gallery users often interact one-handed.
