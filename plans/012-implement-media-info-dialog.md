# Plan 012: Implement MediaInfoDialog in ViewerScreen

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: direction
- **Planned at**: commit `4f07b86`, 2026-06-22

## Why this matters

The Info button in ViewerScreen had an empty click handler (`/* Info dialog */`). Users tapping it expected to see metadata but nothing happened.

## Changes

- `ViewerScreen.kt` — added `showInfoDialog` + `infoItem` state, `MediaInfoDialog` composable with AlertDialog showing type, date, size, dimensions, duration (video), and location; added `InfoRow` helper composable; added `formatDuration` utility
- `strings.xml` — added `close` string resource
