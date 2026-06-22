# Plan 005: Limit full-res image decoding size in ViewerScreen zoomable image

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 4f07b86..HEAD -- app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`
> If `ViewerScreen.kt` changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: XS
- **Risk**: LOW
- **Depends on**: none
- **Category**: perf
- **Planned at**: commit `4f07b86`, 2026-06-22

## Why this matters

The full-screen `ZoomableImage` in `ViewerScreen` creates an `ImageRequest`
with **no size constraint**. Coil decodes the image at its native resolution.
For modern smartphone cameras shooting 48–200 MP, this means decoding a
8000×6000 px bitmap into memory — ~192 MB for a single photo (ARGB_8888,
4 bytes per pixel × 48M pixels). On devices with 4–6 GB of RAM (and the OS
taking ~2 GB), this routinely triggers `OutOfMemoryError` crashes.

Setting a reasonable max decode size (2048 px on the longest edge) drops peak
memory to ~16 MB per image — a 12× reduction — with no visible quality loss
on a phone screen.

## Current state

**`app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`** —
the `ZoomableImage` composable at lines 146–187:

```kotlin
@Composable
private fun ZoomableImage(item: MediaItem) {
    val context = LocalContext.current
    // ...

    Box(/*...*/) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build(),               // ← NO .size() call — decodes full native resolution
            // ...
        )
    }
}
```

All other `ImageRequest.Builder` usages in the codebase **do** set `.size()`:
- `GalleryScreen.kt:228` — `.size(360)` for thumbnails
- `AlbumDetailScreen.kt:99` — `.size(360)` for thumbnails

The viewer is the single omission.

**Repo conventions (match these):**
- `ImageRequest.Builder` calls use chained method calls, one per line
- `.size()` is an integer pixel size (not `Size` object) — see existing usage at `GalleryScreen.kt:229`

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Static check | `grep -n '\.size(' app/src/main/java/.../ViewerScreen.kt` | Shows 1 match on the ImageRequest |
| Build APK | `gradle assembleDebug` | `BUILD SUCCESSFUL`, exit 0 |

## Scope

**In scope** (only file to modify):
- `app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`

**Out of scope** (do NOT touch):
- `GalleryScreen.kt`, `AlbumDetailScreen.kt` — already have `.size(360)` for thumbnails
- Any other file — single-line change

## Steps

### Step 1: Add `.size(2048)` to the zoomable image request

Open `app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt`.

Find the `ImageRequest.Builder` block at line 171–173:

```kotlin
model = ImageRequest.Builder(context)
    .data(item.uri)
    .crossfade(true)
    .build(),
```

Insert `.size(2048)` **after** `.data(item.uri)` and **before** `.crossfade(true)`:

```kotlin
model = ImageRequest.Builder(context)
    .data(item.uri)
    .size(2048)
    .crossfade(true)
    .build(),
```

**Rationale for 2048**: This limits the longest edge to 2048 px, which is
well above any phone's screen resolution (typically ~400 DPI → ~1080–1440 px
logical). The user sees no quality loss, but the decoded bitmap drops from
48M pixels to ~3.1M pixels. On a 1080×2400 screen, 2048 is the ceiling —
Coil will downsample to fit the `ImageRequest` size.

**Do NOT change** any other `.crossfade(true)` or `.data(item.uri)` calls.

**Verify**:
```bash
grep -n '\.size(' app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt
```
→ Shows 1 result: `    .size(2048)`

Also verify the build passes:
```bash
grep -n 'size(2048)' app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt
```
→ `.*.size(2048)` — confirms the exact value.

### Step 2: Build and verify

```bash
gradle assembleDebug
```

Expected output ends with `BUILD SUCCESSFUL`.

If `gradle` is not available, run the static check instead:

```bash
echo "=== Verification ==="
echo "ViewerScreen .size() call:"
grep -c '\.size(' app/src/main/java/io/github/kardeiro/gallery/ui/screen/ViewerScreen.kt
echo "Out-of-scope check:"
git status --short
```

Expected: `ViewerScreen .size() call: 1`, and only `ViewerScreen.kt` modified.

## Test plan

No new tests (no test framework exists). Manual verification:
1. Open a large photo in the viewer — it should display correctly (full-screen, zoomable)
2. Zoom in/out — no visual regression compared to before
3. Rapidly swipe through 20+ photos — no OOM crash (the primary fix)

## Done criteria

ALL must hold:

- [ ] `grep -n '\.size(' app/src/main/java/.../ViewerScreen.kt` shows exactly 1 match containing `.size(2048)`
- [ ] `gradle assembleDebug` exits 0 (or static check passes)
- [ ] No files outside `ViewerScreen.kt` are modified (`git status --short`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- The code in `ZoomableImage` at lines 146–187 doesn't match the excerpts (codebase has drifted)
- A step's verification fails twice after a reasonable fix attempt
- The fix appears to require touching an out-of-scope file
- `coil.compose.AsyncImage` or `coil.request.ImageRequest` is not imported in the file (it is — verified at `ViewerScreen.kt:50-51`)

## Maintenance notes

- If a future plan adds ExoPlayer-based video playback, the video path does
  not use `ImageRequest` and is unaffected by this change.
- The `.size(2048)` value is a sensible default for full-screen viewing. If
  the app ever adds a "zoom to 100%" feature, the `ImageRequest` may need to
  be rebuilt with a larger size on demand — but that's a new feature, not a
  regression concern.
- `.size()` in Coil sets the max pixel size on the **longest edge**, keeping
  aspect ratio. This is the standard approach — no need for `.size(2048, 2048)`.
