# Plan 011: Remove unused imports across source files

## Status

- **Priority**: P3
- **Effort**: XS
- **Risk**: LOW
- **Depends on**: none
- **Category**: tech-debt
- **Planned at**: commit `4f07b86`, 2026-06-22

## Why this matters

Unused imports add noise, slow compilation marginally, and confuse developers reading the code.

## Changes

- `MediaRepository.kt` — removed `android.os.Environment`, `androidx.core.database.getStringOrNull`
- `ViewerScreen.kt` — removed `AnimatedContent`, `systemBarsPadding`, `FileProvider`, `coroutineScope`, `fillMaxWidth`
