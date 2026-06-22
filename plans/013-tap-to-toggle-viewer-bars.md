# Plan 013: Tap-to-toggle UI bars in ViewerScreen

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: direction
- **Planned at**: commit `4f07b86`, 2026-06-22

## Why this matters

`showBars` state existed but was never toggled — bars were always visible in full-screen viewer. Common gallery UX: tap to hide/show UI for immersive viewing.

## Changes

- `ViewerScreen.kt` — `ZoomableImage` accepts `onTap` callback + `detectTapGestures`; `VideoPlayer` wrapped in `Box` with tap handler; both toggle `showBars`
