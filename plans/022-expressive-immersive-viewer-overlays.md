# Plan 022: Redesign viewer chrome as immersive expressive overlays

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report; do not improvise. When done, update the status row for this plan in `plans/README.md` unless a reviewer tells you they maintain the index.
>
> **Drift check (run first)**: `git diff --stat 400d821..HEAD -- app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/theme app/src/main/res/values/strings.xml`
> If any in-scope file changed since this plan was written, compare the "Current state" excerpts against the live code before proceeding; on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: MED
- **Depends on**: plans/020-expressive-material3-theme-foundation.md
- **Category**: ui-ux
- **Planned at**: commit `400d821`, 2026-06-22

## Why this matters

The viewer is the most immersive part of a gallery app, but its controls currently use a standard top app bar over a full-screen pager. That is functional, yet visually heavy for photo viewing and not especially expressive. Material You expressive overlays can keep controls reachable while giving media more presence: translucent surface containers, large touch targets, clear hierarchy, and smoother show/hide behavior.

## Current state

Relevant file:

- `ViewerScreen.kt` — viewer pager, top app bar actions, info dialog, delete dialog, image zoom, and video playback.

Current top bar excerpt:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt:89-131
Scaffold(
    topBar = {
        if (showBars && mediaItems.isNotEmpty()) {
            TopAppBar(
                title = {
                    Text("${pagerState.currentPage + 1} / ${mediaItems.size}")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { ... }) { Icon(Icons.Filled.Share, ...) }
                    IconButton(onClick = { ... }) { Icon(Icons.Filled.Info, ...) }
                    IconButton(onClick = { ... }) { Icon(Icons.Filled.Delete, ...) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }
    },
    containerColor = MaterialTheme.colorScheme.background
)
```

Dialogs currently use default `AlertDialog` and hardcoded labels in info rows:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt:298-313
Column {
    InfoRow(label = "Type", value = item.mediaType.name)
    InfoRow(label = "Date", value = item.formattedDate)
    InfoRow(label = "Size", value = item.formattedSize)
    InfoRow(label = "Dimensions", value = "${item.width} x ${item.height} px")
```

Repo conventions to match:

- Use Material 3 components and semantic color roles.
- Keep viewer controls simple: back, share, info, delete.
- Use string resources for visible text when adding or replacing labels.
- Preserve tap-to-toggle behavior from plan 013 and video optimizations from plan 019.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Build | `./gradlew assembleDebug` | exit 0 and `BUILD SUCCESSFUL` |
| Status | `git status --short` | only intended files changed |

## Scope

**In scope**:

- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/theme/*` only to consume plan 020 tokens.
- `app/src/main/res/values/strings.xml` for new labels.
- `plans/README.md`

**Out of scope**:

- Changing video playback engine.
- Changing delete/share functionality.
- Adding edit tools, slideshow, filters, or metadata editing.
- Changing navigation routes.

## Git workflow

- Work after plan 020 has landed.
- Commit message suggestion: `autonomous-improve: redesign viewer expressive overlays`.
- Do not push unless instructed.

## Steps

### Step 1: Replace the standard top app bar with expressive overlay chrome

In `ViewerScreen.kt`, keep `Scaffold` if useful, but change the visible controls so media feels immersive:

- Use a root `Box` inside the scaffold content so controls overlay the pager.
- Top overlay: a rounded translucent `Surface` or `Card` with back button and page counter.
- Bottom or side action rail: rounded translucent `Surface` with share, info, and delete icon buttons.
- Use `MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)` or `surfaceContainerHigh` with a scrim-like alpha; ensure contrast in dark mode.
- Each icon button must retain a content description and 48dp touch target.
- Keep `showBars` behavior: overlays appear only when `showBars` is true and media exists.

Do not remove `onBack`, share, info, or delete logic.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 2: Animate overlay visibility with Material motion

Use Compose animation APIs such as `AnimatedVisibility`, `fadeIn`, `fadeOut`, `slideInVertically`, and `slideOutVertically` for the top and action overlays. Keep durations in the 150-300ms range and use default Material-feeling easing if available.

Avoid animating layout size. Animate opacity and translation only.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 3: Improve info dialog hierarchy and localization

Update `MediaInfoDialog` to feel like a Material 3 information sheet/dialog:

- Keep `AlertDialog` if it remains simple, but use clearer section spacing and `HorizontalDivider` only where it reinforces grouping.
- Move hardcoded labels (`Type`, `Date`, `Size`, `Dimensions`, `Duration`, `Location`) to `strings.xml`.
- Use `MaterialTheme.typography.labelMedium` for labels and `bodyMedium` or `bodyLarge` for values.
- Avoid values running off-screen: use wrapping or right-aligned values only where safe.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 4: Make delete confirmation visually safer

Update `DeleteConfirmDialog` to make destructive action clarity stronger:

- Keep the existing confirmation text.
- Use `MaterialTheme.colorScheme.error` for the destructive confirm text.
- Put delete as the confirm action and cancel as dismiss; keep both touch targets clear.
- If adding an icon, use a Material icon and a string content description.
- Do not perform deletion without confirmation.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 5: Update index

After the build passes, mark plan 022 as `DONE ✅` in `plans/README.md`.

**Verify**: `git status --short` -> only in-scope files are modified.

## Test plan

- Build: `./gradlew assembleDebug` -> `BUILD SUCCESSFUL`.
- Manual photo viewer: tap image to show/hide overlays; animation should feel quick and interruptible.
- Manual video viewer: tap video area to show/hide overlays; PlayerView controller should still work.
- Manual dark mode: overlay text/icons must remain readable over bright and dark media.
- Manual large font: page counter and dialog rows should not overlap actions.
- Manual delete/share/info: all actions still perform the same behavior as before.

## Done criteria

- [ ] `./gradlew assembleDebug` exits 0 and prints `BUILD SUCCESSFUL`.
- [ ] Viewer no longer relies on a full-width standard `TopAppBar` as the primary visual chrome.
- [ ] Controls use expressive rounded overlays with semantic Material colors.
- [ ] Overlay visibility animates with opacity/translation in 150-300ms range.
- [ ] Info dialog labels are localized in `strings.xml`.
- [ ] Delete confirmation remains explicit and visually destructive.
- [ ] `plans/README.md` marks plan 022 as `DONE ✅`.

## STOP conditions

Stop and report back if:

- Plan 019 has been reverted and video pages again pre-compose players; resolve that first.
- The overlay approach breaks tap-to-toggle or PlayerView controller interaction in a way that needs a new gesture architecture.
- Required animation APIs are unavailable in the current Compose version.
- Build fails twice after reasonable import/signature fixes.

## Maintenance notes

- Reviewers should test the overlays over very light photos because translucent surfaces can lose contrast.
- If future editing tools are added, extend the action overlay rather than reintroducing a heavy top app bar.
- Keep destructive actions visually separated from share/info to prevent accidental taps.
