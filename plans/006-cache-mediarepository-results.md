# Plan 006: Cache MediaRepository.loadMedia() to eliminate redundant MediaStore queries

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 4f07b86..HEAD -- app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt`
> If `MediaRepository.kt` changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: MED
- **Depends on**: none
- **Category**: perf
- **Planned at**: commit `4f07b86`, 2026-06-22

## Why this matters

`MediaRepository.loadMedia()` does a full `ContentResolver.query()` against
**both** `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` and
`MediaStore.Video.Media.EXTERNAL_CONTENT_URI` **every time** it is called.
On app launch it is invoked 3+ times:

1. `GalleryScreen.kt:110` — initial data load
2. `AlbumDetailScreen.kt:55` — when user opens an album
3. `ViewerScreen.kt:71` — when user opens the viewer
4. `ViewerScreen.kt:113` — after delete, to refresh

For a user with 5000+ media items, each call iterates ~10000+ cursor rows
(5000 images + 5000 videos). This adds 200–800 ms of UI-thread blocking
**per call**, causing repeated empty states and visible jank during
navigation. The data is the same until the user takes/deletes a photo.

A simple in-memory cache avoids the redundant queries entirely while keeping
the fix local to `MediaRepository` — no architectural changes needed.

## Current state

**`app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt`** —
the full class (161 lines):

Key facts:
- `loadMedia()` (lines 14–18) queries both Images and Video stores every call
- `deleteMedia()` (lines 154–160) deletes via `ContentResolver.delete()` but
  does not invalidate any cache
- There is no state — the class is stateless, instantiated fresh or via
  `remember` in each screen

All four call sites invoke `loadMedia()` and expect fresh data:

`GalleryScreen.kt:110`:
```kotlin
if (hasPermission) {
    mediaItems = repository.loadMedia()
    isLoading = false
```

`AlbumDetailScreen.kt:55`:
```kotlin
LaunchedEffect(bucketId) {
    mediaItems = repository.loadMedia().filter { it.bucketId == bucketId }
```

`ViewerScreen.kt:71`:
```kotlin
LaunchedEffect(Unit) {
    val all = repository.loadMedia()
    mediaItems = if (bucketId != null) all.filter { it.bucketId == bucketId } else all
```

`ViewerScreen.kt:113` (after delete):
```kotlin
repository.deleteMedia(it.uri)
mediaItems = repository.loadMedia()
```

**Repo conventions (match these):**
- Class style: single file per class, no ViewModels yet, data access via
  `MediaRepository(context)` with `remember`
- No coroutines are used in the repository — queries run on the calling
  thread (main thread in all cases). The fix preserves this.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Static verify | `grep -n 'cache\|CACHE\|cached\|needsRefresh\|invalidate' MediaRepository.kt` | Shows cache fields + methods |
| Build APK | `gradle assembleDebug` | `BUILD SUCCESSFUL`, exit 0 |

## Scope

**In scope** (only file to modify):
- `app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt`

**Out of scope** (do NOT touch):
- Any screen file (`GalleryScreen.kt`, `AlbumDetailScreen.kt`, `ViewerScreen.kt`) —
  they call `repository.loadMedia()` and benefit automatically from the cache
- `MediaItem.kt`, `Album` — no data model changes
- Any other file

## Steps

### Step 1: Add cached state and update `loadMedia()`

Open `app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt`.

Add two private fields at the top of the class, after the opening brace:

```kotlin
class MediaRepository(private val context: Context) {

    private var cachedMedia: List<MediaItem>? = null
```

Then modify `loadMedia()` to use the cache. Replace the existing lines 14–18:

```kotlin
    fun loadMedia(): List<MediaItem> {
        val images = queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)
        val videos = queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DURATION)
        return (images + videos).sortedByDescending { it.dateTaken }
    }
```

With:

```kotlin
    fun loadMedia(): List<MediaItem> {
        if (cachedMedia != null) return cachedMedia!!

        val images = queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)
        val videos = queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DURATION)
        return (images + videos).sortedByDescending { it.dateTaken }
            .also { cachedMedia = it }
    }
```

**Verify**:
```bash
grep -n 'cachedMedia' app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt
```
→ Shows at least 3 references (field declaration, null check, assignment via `.also`).

### Step 2: Invalidate cache after delete

Find the `deleteMedia()` method at lines 154–160:

```kotlin
    fun deleteMedia(uri: Uri): Boolean {
        return try {
            context.contentResolver.delete(uri, null, null) > 0
        } catch (_: SecurityException) {
            false
        }
    }
```

Add a cache invalidation after a successful delete. Change to:

```kotlin
    fun deleteMedia(uri: Uri): Boolean {
        return try {
            val deleted = context.contentResolver.delete(uri, null, null) > 0
            if (deleted) cachedMedia = null
            deleted
        } catch (_: SecurityException) {
            false
        }
    }
```

**Verify**:
```bash
grep -n 'cachedMedia = null' app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt
```
→ Shows 1 result in `deleteMedia()`.

### Step 3: Add an explicit `invalidateCache()` for future use (optional but future-proof)

Add a public method that screens can call if they ever need to force a refresh
(such as when the app resumes from background):

```kotlin
    fun invalidateCache() {
        cachedMedia = null
    }
```

Place it right after `loadMedia()` and before `queryMedia()` (between lines 18
and 20 in the original file). This method is **not called anywhere yet** — it
is scaffolding for future use (e.g., a planned ContentObserver).

**Verify**:
```bash
grep -n 'invalidateCache' app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt
```
→ Shows 1 result with the method signature.

### Step 4: Build and verify

```bash
gradle assembleDebug
```

Expected output ends with `BUILD SUCCESSFUL`.

If `gradle` is not available, run the static check:

```bash
echo "=== Verification ==="
echo "Field declaration:"
grep -c 'cachedMedia' app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt
echo "Cache check:"
grep -c 'cachedMedia != null' app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt
echo "Cache set:"
grep -c 'cachedMedia = it' app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt
echo "Cache invalidate on delete:"
grep -c 'cachedMedia = null' app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt
echo "Invalidate method:"
grep -c 'invalidateCache' app/src/main/java/io/github/kardeiro/gallery/data/MediaRepository.kt
echo "Out-of-scope check:"
git status --short
```

Expected: 5 lines, each showing a count ≥ 1, and only `MediaRepository.kt`
modified.

## Test plan

No new tests (no test framework exists). Manual verification:
1. Open the app — gallery loads once (verify with Logcat or profiler that
   ContentResolver.query is called only once, not 3 times)
2. Navigate to Albums → tap an album → loads instantly from cache
3. Tap a photo → viewer opens instantly from cache
4. Press Delete → cache invalidated → next `loadMedia()` re-queries
5. Go back to gallery → grid refreshes with item removed

## Done criteria

ALL must hold:

- [ ] `grep -n 'cachedMedia' app/src/main/java/.../MediaRepository.kt` shows field, null check, `.also` assignment
- [ ] `grep -n 'cachedMedia = null' app/src/main/java/.../MediaRepository.kt` shows invalidation in `deleteMedia()`
- [ ] `grep -n 'invalidateCache' app/src/main/java/.../MediaRepository.kt` shows public method
- [ ] `gradle assembleDebug` exits 0 (or static check passes)
- [ ] No files outside `MediaRepository.kt` are modified (`git status --short`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- The code at the locations in "Current state" doesn't match the excerpts
  (the codebase has drifted since this plan was written).
- A step's verification fails twice after a reasonable fix attempt.
- The fix appears to require touching an out-of-scope file.
- `gradle assembleDebug` fails with a compilation error — if the `!!` on
  `cachedMedia!!` is flagged as a warning/error by the Kotlin compiler,
  change to `cachedMedia ?: emptyList()` — but `cachedMedia` is only
  `null` before the first load, and `loadMedia()` always sets it; the `!!`
  is safe because the cache-hit path returns before any null assignment.
  Still, `requireNotNull(cachedMedia)` is an acceptable alternative.

## Maintenance notes

- This cache is **in-memory only** and dies with the process. On process
  restart, the first `loadMedia()` call will re-query. This is the correct
  behavior — persistent caches of MediaStore data go stale.
- If the app later registers a `ContentObserver` on `MediaStore.Images.Media`
  and `MediaStore.Video.Media` URIs, the observer should call
  `repository.invalidateCache()` when content changes. This plan exposes
  `invalidateCache()` for exactly that purpose.
- The `loadAlbums()` method is **not cached** by this plan — it should be
  addressed separately (it has its own performance issue: scanning all rows
  for bucket info).
- The `!!` non-null assertion is intentional: the cache-hit path guarantees
  `cachedMedia` is non-null, and the assertion would only fail if there is a
  logic bug. It is safe.
