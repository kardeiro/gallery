# Plan 009: Move SimpleDateFormat to companion object to avoid re-instantiation

## Status

- **Priority**: P3
- **Effort**: XS
- **Risk**: LOW
- **Depends on**: none
- **Category**: perf
- **Planned at**: commit `4f07b86`, 2026-06-22

## Why this matters

`formattedDate` created a new `SimpleDateFormat("MMM dd, yyyy")` on every property access — an expensive object that parses the format pattern string each time.

**Fix**: Move to companion object singleton.
