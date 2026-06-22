# Plan 002: Replace hardcoded string literals with string resource references

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise.
>
> **Drift check (run first)**: `git diff --stat e9bda70..HEAD -- app/src/main/java/io/github/kardeiro/gallery/ui/screen/ app/src/main/res/values/strings.xml`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: tech-debt
- **Planned at**: commit `e9bda70`, 2026-06-21

## Why this matters

The app has 11 string resources defined in `strings.xml` but every screen uses
hardcoded string literals (`Text("Gallery")`, `contentDescription = "Back"`,
etc.) instead of referencing them via `stringResource(R.string.xxx)`. This
means:
- The app cannot be localized (translators would need to edit Kotlin files)
- Inconsistent pattern — half the strings exist as resources but go unused
- A string mismatch: `strings.xml` has a long empty-state message while the
  code shows a shorter one

This plan migrates all hardcoded user-visible strings in 4 screen files to
`stringResource()` calls and fills the gaps in `strings.xml`.

## Current state

### `app/src/main/res/values/strings.xml` (existing resources)

```xml
<resources>
    <string name="app_name">Gallery</string>
    <string name="gallery">Gallery</string>
    <string name="albums">Albums</string>
    <string name="permission_required">Permission Required</string>
    <string name="permission_message">Allow access to photos and videos to view your gallery.</string>
    <string name="grant_permission">Grant Permission</string>
    <string name="no_media">No media found</string>
    <string name="no_media_description">Your gallery is empty. Take some photos or videos to get started.</string>
    <string name="delete">Delete</string>
    <string name="share">Share</string>
    <string name="info">Info</string>
</resources>
```

### Strings missing from `strings.xml` that are hardcoded in code

| String | Used in | Occurrences |
|--------|---------|-------------|
| `"Photos"` | GalleryScreen (contentDescription + label) | 2 |
| `"Back"` | AlbumScreen, ViewerScreen, AlbumDetailScreen (contentDescription) | 3 |

### String mismatch

- Code says: `"Your gallery is empty."` (GalleryScreen.kt:302)
- Resource says: `"Your gallery is empty. Take some photos or videos to get started."` (`R.string.no_media_description`)
- This plan updates the resource to match the shorter code version.

### Hardcoded strings per file (all must change)

**GalleryScreen.kt** — no `stringResource` import exists:
- Line 120: `Text("Gallery")` → `Text(stringResource(R.string.gallery))`
- Line 138: `contentDescription = "Photos"` → `contentDescription = stringResource(R.string.photos)`
- Line 141: `label = { Text("Photos") }` → `label = { Text(stringResource(R.string.photos)) }`
- Line 152: `contentDescription = "Albums"` → `contentDescription = stringResource(R.string.albums)`
- Line 155: `label = { Text("Albums") }` → `label = { Text(stringResource(R.string.albums)) }`
- Line 261: `text = "Permission Required"` → `text = stringResource(R.string.permission_required)`
- Line 266: `text = "Allow access to photos and videos to view your gallery."` → `text = stringResource(R.string.permission_message)`
- Line 273: `Text("Grant Permission")` → `Text(stringResource(R.string.grant_permission))`
- Line 297: `text = "No media found"` → `text = stringResource(R.string.no_media)`
- Line 302: `text = "Your gallery is empty."` → `text = stringResource(R.string.no_media_description)`

**AlbumScreen.kt** — no `stringResource` import exists:
- Line 65: `Text("Albums")` → `Text(stringResource(R.string.albums))`
- Line 68: `contentDescription = "Back"` → `contentDescription = stringResource(R.string.back)`
- Line 138: `text = "${album.itemCount} items"` → `text = stringResource(R.string.items_count, album.itemCount)`

**ViewerScreen.kt** — no `stringResource` import exists:
- Line 78: `contentDescription = "Back"` → `contentDescription = stringResource(R.string.back)`
- Line 93: `contentDescription = "Share"` → `contentDescription = stringResource(R.string.share)`
- Line 96: `contentDescription = "Info"` → `contentDescription = stringResource(R.string.info)`
- Line 105: `contentDescription = "Delete"` → `contentDescription = stringResource(R.string.delete)`
- Line 90: `Intent.createChooser(shareIntent, "Share")` — this is a system chooser title, NOT a composable string. Leave it hardcoded (it's a UI affordance for the share sheet, not a display string).

**AlbumDetailScreen.kt** — no `stringResource` import exists:
- Line 65: `contentDescription = "Back"` → `contentDescription = stringResource(R.string.back)`

### Repository conventions

- Imports are grouped: `androidx.*` then `coil.*` then `io.github.kardeiro.*`
- Import `stringResource` goes with other `androidx.compose.ui` imports
  (`import androidx.compose.ui.res.stringResource`)
- The existing pattern in other files already uses `R.string.app_name` in
  `AndroidManifest.xml` via `@string/app_name` — this plan extends that to
  Compose code.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Build APK | `gradle assembleDebug` | `BUILD SUCCESSFUL`, exit 0 |

## Scope

**In scope** (the only files you should modify):
- `app/src/main/res/values/strings.xml` — add missing strings, fix mismatch
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt`

**Out of scope** (do NOT touch):
- `NavGraph.kt` — no hardcoded user-visible strings
- `MediaRepository.kt`, `MediaItem.kt`, `GalleryApp.kt`, `MainActivity.kt` — not related
- Theme files (`Color.kt`, `Theme.kt`, `Type.kt`) — not related
- The `Intent.createChooser` chooser title `"Share"` at ViewerScreen.kt:90 — intentional, it's a system UI label, not a composable string

## Git workflow

- Branch: `advisor/002-migrate-strings`
- Commit per file group (1 commit for xml + GalleryScreen, 1 commit for the rest)
- Message style: `fix: replace hardcoded strings with stringResource references`

## Steps

### Step 1: Update `strings.xml`

Open `app/src/main/res/values/strings.xml`.

Make three changes:

1. Update `no_media_description` to match the shorter code version:
   ```xml
   <string name="no_media_description">Your gallery is empty.</string>
   ```

2. Add `photos` (missing):
   ```xml
   <string name="photos">Photos</string>
   ```

3. Add `back` (missing):
   ```xml
   <string name="back">Back</string>
   ```

4. Add `items_count` with a format placeholder:
   ```xml
   <string name="items_count">%d items</string>
   ```

**Verify**:
```bash
grep -E 'name="(photos|back|items_count|no_media_description)"' app/src/main/res/values/strings.xml
```
→ Shows 4 lines, each with the correct value.

### Step 2: Update GalleryScreen.kt

Open `app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt`.

1. Add import after the existing `import androidx.compose.ui.platform.LocalContext` line:
   ```kotlin
   import androidx.compose.ui.res.stringResource
   ```

2. Make these replacements (exact find strings):

   a. Line 120: `title = { Text("Gallery") }` →
      `title = { Text(stringResource(R.string.gallery)) }`

   b. Lines 138–139: Replace the `contentDescription = "Photos"` and
      `label = { Text("Photos") }` block. The exact code around line 135–141 is:
      ```
      Icon(
          Icons.Filled.PhotoLibrary,
          contentDescription = "Photos"
      )
      },
      label = { Text("Photos") }
      ```
      Replace `"Photos"` with `stringResource(R.string.photos)` in both places.

   c. Lines 152–155: Same pattern, replace `"Albums"` with `stringResource(R.string.albums)`.

   d. Line 261: `text = "Permission Required"` →
      `text = stringResource(R.string.permission_required)`

   e. Line 266: `text = "Allow access to photos and videos to view your gallery."` →
      `text = stringResource(R.string.permission_message)`

   f. Line 273: `Text("Grant Permission")` →
      `Text(stringResource(R.string.grant_permission))`

   g. Line 297: `text = "No media found"` →
      `text = stringResource(R.string.no_media)`

   h. Line 302: `text = "Your gallery is empty."` →
      `text = stringResource(R.string.no_media_description)`

**Verify**:
```bash
grep -c 'stringResource' app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt && grep -c 'stringResource\|import.*stringResource' app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt
```
→ Shows nonzero counts (at least 1 import + 10 stringResource calls).

Also verify no hardcoded display strings remain:
```bash
grep -n 'Text("[A-Z]' app/src/main/java/io/github/kardeiro/gallery/ui/screen/GalleryScreen.kt
```
→ Should show 0 matches (no `Text("` starting with uppercase A-Z).

### Step 3: Update AlbumScreen.kt

Open `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt`.

1. Add import after `import androidx.compose.ui.platform.LocalContext`:
   ```kotlin
   import androidx.compose.ui.res.stringResource
   ```

2. Make replacements:

   a. Line 65: `title = { Text("Albums") }` →
      `title = { Text(stringResource(R.string.albums)) }`

   b. Line 68: `contentDescription = "Back"` →
      `contentDescription = stringResource(R.string.back)`

   c. Line 138: `text = "${album.itemCount} items"` →
      `text = stringResource(R.string.items_count, album.itemCount)`

**Verify**:
```bash
grep -c 'stringResource' app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumScreen.kt
```
→ Shows at least 3 (1 import + 3 calls).

### Step 4: Update ViewerScreen.kt

Open `app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`.

1. Add import after existing `import androidx.compose.ui.layout.ContentScale`:
   ```kotlin
   import androidx.compose.ui.res.stringResource
   ```

2. Make replacements:

   a. Line 78: `contentDescription = "Back"` →
      `contentDescription = stringResource(R.string.back)`

   b. Line 93: `contentDescription = "Share"` →
      `contentDescription = stringResource(R.string.share)`

   c. Line 96: `contentDescription = "Info"` →
      `contentDescription = stringResource(R.string.info)`

   d. Line 105: `contentDescription = "Delete"` →
      `contentDescription = stringResource(R.string.delete)`

3. Do NOT change line 90 (`Intent.createChooser(shareIntent, "Share")`).

**Verify**:
```bash
grep -c 'stringResource' app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt
```
→ Shows at least 5 (1 import + 4 contentDescription calls).

Verify only Intent.createChooser "Share" remains as the sole hardcoded string:
```bash
grep -n '"Back"\|"Share"\|"Info"\|"Delete"' app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt
```
→ Should show 0 matches (if the Intent "Share" was changed, fix it back — it must stay hardcoded).

### Step 5: Update AlbumDetailScreen.kt

Open `app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt`.

1. Add import after `import androidx.compose.ui.platform.LocalContext`:
   ```kotlin
   import androidx.compose.ui.res.stringResource
   ```

2. Line 65: `contentDescription = "Back"` →
   `contentDescription = stringResource(R.string.back)`

**Verify**:
```bash
grep -c 'stringResource' app/src/main/java/io/github/kardeiro/gallery/ui/screen/AlbumDetailScreen.kt
```
→ Shows at least 2 (1 import + 1 call).

### Step 6: Build and verify

```bash
gradle assembleDebug
```

Expected output ends with `BUILD SUCCESSFUL`.

> If the build fails with "Unresolved reference: R", check that the import
> added is `import androidx.compose.ui.res.stringResource` (not
> `androidx.compose.ui.resources.stringResource`).

## Test plan

No new tests. Verification is the compilation step. Manually verify that
displayed strings match expectations when running the app (e.g. bottom bar
shows "Photos" and "Albums", permission card shows "Permission Required", etc.).

## Done criteria

ALL must hold:

- [ ] `gradle assembleDebug` exits 0
- [ ] `strings.xml` has 14 entries (3 added: `photos`, `back`, `items_count` + 1 modified: `no_media_description`)
- [ ] `grep -rn 'stringResource' app/src/main/java/io/github/kardeiro/gallery/ui/screen/` returns at least 16 lines (4 files × import + 12 calls)
- [ ] No hardcoded display strings remain: `grep -rn 'Text("[A-Z]' app/src/main/java/io/github/kardeiro/gallery/ui/screen/` returns 0 matches
- [ ] No files outside the in-scope list are modified (`git status` shows only the 5 listed files)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- The code at the locations in "Current state" doesn't match the excerpts
  (the codebase has drifted since this plan was written).
- A step's verification fails twice after a reasonable fix attempt.
- The fix appears to require touching an out-of-scope file.
- The `Intent.createChooser` "Share" string was accidentally changed
  (it must stay hardcoded — it's a system API parameter, not a display string).
- A Kotlin file already has `import androidx.compose.ui.res.stringResource`
  from a previous change — if so, skip adding a duplicate import.

## Maintenance notes

- When adding new UI strings to future screens, always define the string in
  `strings.xml` first, then reference it via `stringResource(R.string.xxx)`.
- Format strings (like `%d items`) allow proper localization — translators
  can reorder the placeholder. Keep using this pattern for any string with
  dynamic values.
- If the app ever adds locale support (e.g. `values-pt/strings.xml` for
  Portuguese), only the XML files need translation; no Kotlin code changes.
- The `Intent.createChooser` `"Share"` string is a system UI hint, not an
  app display string. Translation of that string is handled by the Android
  OS (it shows the share sheet title in the user's locale automatically).
