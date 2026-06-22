# Plan 015: Request READ_MEDIA_VIDEO permission on Android 13+ for video loading

## Status

- **Priority**: P1
- **Effort**: XS
- **Risk**: LOW
- **Depends on**: none
- **Category**: direction
- **Planned at**: commit `098c54e`, 2026-06-22

## Why this matters

On Android 13+ (API 33+), `READ_MEDIA_VIDEO` is required alongside `READ_MEDIA_IMAGES` to query `MediaStore.Video.Media`. The app only requested `READ_MEDIA_IMAGES`, so videos silently failed to load on modern devices.

## Changes

- `GalleryScreen.kt` — switch from single `RequestPermission` to `RequestMultiplePermissions()`; check both `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` on Tiramisu+
