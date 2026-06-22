# Plan 014: Add delete confirmation dialog in ViewerScreen

## Status

- **Priority**: P1
- **Effort**: XS
- **Risk**: LOW
- **Depends on**: none
- **Category**: direction
- **Planned at**: commit `4f07b86`, 2026-06-22

## Why this matters

Delete button in ViewerScreen deleted immediately with no confirmation — accidental taps could permanently lose photos.

## Changes

- `ViewerScreen.kt` — added `showDeleteDialog`/`deleteTargetItem` state, `DeleteConfirmDialog` composable with confirm/dismiss
- `strings.xml` — added `cancel`, `delete_confirm` resources
