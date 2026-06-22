# Plan 007: Replace O(n) indexOf with O(1) ID-to-index map in grid click handlers

## Status

- **Priority**: P2
- **Effort**: XS
- **Risk**: LOW
- **Depends on**: none
- **Category**: perf
- **Planned at**: commit `4f07b86`, 2026-06-22

## Why this matters

Every thumbnail tap in GalleryScreen and AlbumDetailScreen calls `mediaItems.indexOf(item)`, which is an O(n) linear scan through the entire list. For users with 5000+ items, each tap scans ~5000 items, causing a visible micro-jank on the click-to-viewer transition.

**Fix**: Derive a `Map<Long, Int>` (item.id → index) via `remember(mediaItems)` and use O(1) map lookup instead.

## Changes

- `GalleryScreen.kt` — added `idIndexMap`, replaced `indexOf` call
- `AlbumDetailScreen.kt` — added `idIndexMap`, replaced `indexOf` call
