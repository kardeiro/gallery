# Plan 008: Remove crossfade animation from grid thumbnails

## Status

- **Priority**: P3
- **Effort**: XS
- **Risk**: LOW
- **Depends on**: none
- **Category**: perf
- **Planned at**: commit `4f07b86`, 2026-06-22

## Why this matters

Every grid thumbnail had `.crossfade(true)`, causing GPU blend operations on every image load. During fast scrolling, dozens of thumbnails load per frame, creating unnecessary GPU overhead.

**Fix**: Remove `.crossfade(true)` from grid thumbnails only. Crossfade retained in ViewerScreen (single image, not in a scrolling grid).
