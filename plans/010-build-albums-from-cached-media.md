# Plan 010: Build album list from cached in-memory media instead of MediaStore scan

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: 006 (cachedMedia must exist)
- **Category**: perf
- **Planned at**: commit `4f07b86`, 2026-06-22

## Why this matters

`loadAlbums()` ran two full database scans (images + videos) every call, iterating every single media item just to count items per bucket. Since `cachedMedia` is already in memory (Plan 006), `loadAlbums()` can build the album list from memory in one O(n) pass — zero database I/O.

## Changes

- `MediaRepository.kt` — `loadAlbums()` checks `cachedMedia` first, builds albums via `groupBy { it.bucketId }`; falls back to DB query if cache is null.
