# Plan 023: Add expressive empty, permission, loading, and feedback states

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report; do not improvise. When done, update the status row for this plan in `plans/README.md` unless a reviewer tells you they maintain the index.
>
> **Drift check (run first)**: `git diff --stat 400d821..HEAD -- app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt app/src/main/res/values/strings.xml app/src/main/java/io/github/kardeiro/gallery/ui/theme`
> If any in-scope file changed since this plan was written, compare the "Current state" excerpts against the live code before proceeding; on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: MED
- **Depends on**: plans/020-expressive-material3-theme-foundation.md
- **Category**: ui-ux
- **Planned at**: commit `400d821`, 2026-06-22

## Why this matters

The app has functional permission, loading, and empty states, but they are visually generic and provide limited recovery guidance. A gallery app often opens into permission prompts, empty albums, slow scans, or deletion feedback; those moments shape trust. Expressive Material 3 states should be calm, clear, accessible, and actionable without adding tracking or complexity.

## Current state

Permission state excerpt:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt:261-300
private fun PermissionPlaceholder(
    onRequestPermission: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Card(
            modifier = Modifier.padding(32.dp)
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
```

Empty state excerpt:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt:305-330
private fun EmptyGalleryPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
```

Loading state excerpt:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt:173-180
isLoading -> {
    LinearProgressIndicator(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
    )
}
```

Delete flow excerpt:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt:142-151
DeleteConfirmDialog(
    onConfirm = {
        val targetItem = deleteTargetItem ?: return@DeleteConfirmDialog
        repository.deleteMedia(targetItem.uri)
        showDeleteDialog = false
        deleteTargetItem = null
        scope.launch {
            mediaItems = repository.loadMedia()
        }
    },
```

Repo conventions to match:

- Use Material 3 components and string resources.
- Keep privacy-first tone: no cloud, sync, analytics, account, or tracking language.
- Use vector icons from existing Material icon dependency.
- Keep data layer unchanged unless required for a UI feedback state.

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
- `app/src/main/res/values/strings.xml`
- `app/src/main/java/io/github/kardeiro/gallery/ui/theme/*` only to consume plan 020 tokens.
- `plans/README.md`

**Out of scope**:

- Adding analytics, onboarding flows, cloud backup, or account features.
- Changing permission semantics beyond presentation.
- Replacing dialogs with a full custom framework.
- Adding tests infrastructure.

## Git workflow

- Work after plan 020 has landed.
- Commit message suggestion: `autonomous-improve: add expressive UI states`.
- Do not push unless instructed.

## Steps

### Step 1: Build a reusable expressive state panel pattern

In `GalleryScreen.kt`, add a private composable such as `ExpressiveStateCard` or refactor the existing placeholders so permission and empty states share a visual structure:

- Centered card/surface with expressive corner radius.
- Icon container using `primaryContainer` or `secondaryContainer`.
- Title with `titleLarge` or `headlineSmall` depending on space.
- Body copy with `bodyMedium` and `onSurfaceVariant`.
- Optional action button with at least 48dp height.
- Responsive width using `fillMaxWidth()` plus padding, not fixed width.

Use existing strings first. Add new strings only when copy changes.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 2: Improve permission guidance

Update `PermissionPlaceholder`:

- Make the primary action visually prominent and clearly labeled.
- Keep privacy-first copy: explain that access is local and needed to display photos/videos.
- Preserve `multiplePermissionLauncher.launch(permissions)` behavior.
- Ensure the icon is decorative only if the title/body already explain the state; otherwise add a content description.

If changing copy, update `strings.xml`.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 3: Improve empty gallery and empty album states

Update `EmptyGalleryPlaceholder` to use the expressive state panel. Add an empty state to `AlbumScreen` and/or `AlbumDetailScreen` if they currently show a blank grid when `albums` or `mediaItems` are empty.

Requirements:

- Empty album list should explain that albums appear after media access and indexing.
- Empty album detail should explain that this album has no visible media.
- Use string resources for new copy.
- Avoid blaming the user or implying cloud sync.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 4: Replace bare top loading bar with expressive loading state

For initial gallery load, replace or augment the top `LinearProgressIndicator` with a centered loading state using `CircularProgressIndicator` or a low-noise skeleton-like surface. Keep it lightweight; do not add shimmer dependencies.

Requirements:

- Loading should not look like an error or empty state.
- Keep progress accessible by adding text such as "Loading media" in `strings.xml`.
- Avoid continuous complex animations beyond standard Material progress.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 5: Add delete result feedback

In `ViewerScreen.kt`, after delete confirmation, show lightweight feedback using Material 3 `SnackbarHost` if practical:

- On success: brief message like `Item deleted`.
- On failure: message like `Could not delete item`.
- Do not implement undo unless Android scoped storage delete behavior is fully understood and reversible in this codebase.
- Use `SnackbarHostState` and `rememberCoroutineScope`; avoid toasts unless snackbar integration becomes too invasive.

If `repository.deleteMedia(targetItem.uri)` returns `false`, preserve the item list and show failure feedback.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 6: Update index

After the build passes, mark plan 023 as `DONE ✅` in `plans/README.md`.

**Verify**: `git status --short` -> only in-scope files are modified.

## Test plan

- Build: `./gradlew assembleDebug` -> `BUILD SUCCESSFUL`.
- Manual no-permission state: deny permissions and verify the state card is readable and the action still requests permissions.
- Manual empty state: test on an emulator/profile with no media and verify copy/action layout.
- Manual album empty state: open albums when no albums exist or simulate empty list if possible.
- Manual delete success/failure: delete a media item and verify feedback appears; test failure if scoped storage prevents deletion.
- Manual dark mode and large font: cards, buttons, and snackbar remain readable and unclipped.

## Done criteria

- [ ] `./gradlew assembleDebug` exits 0 and prints `BUILD SUCCESSFUL`.
- [ ] Permission and empty states use a shared expressive Material 3 visual pattern.
- [ ] Initial loading has clear text and does not rely only on a thin top bar.
- [ ] Album and album-detail empty states no longer render as blank grids.
- [ ] Delete success/failure feedback is visible and localized.
- [ ] No analytics/cloud/account copy is introduced.
- [ ] `plans/README.md` marks plan 023 as `DONE ✅`.

## STOP conditions

Stop and report back if:

- Snackbar integration conflicts with the viewer overlay redesign from plan 022.
- Delete result behavior requires changing repository semantics beyond using the existing Boolean return.
- Plan 020 tokens do not exist yet.
- Build fails twice after reasonable import/signature fixes.

## Maintenance notes

- Reuse the expressive state card for future search/filter empty states.
- If Android delete permissions are later improved with `RecoverableSecurityException`, update the failure snackbar copy to guide the user through system confirmation.
- Keep state copy short; gallery users are usually trying to get back to media quickly.
