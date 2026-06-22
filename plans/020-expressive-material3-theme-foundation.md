# Plan 020: Introduce an expressive Material 3 theme foundation

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report; do not improvise. When done, update the status row for this plan in `plans/README.md` unless a reviewer tells you they maintain the index.
>
> **Drift check (run first)**: `git diff --stat 400d821..HEAD -- app/src/main/java/io/github/kardeiro/gallery/ui/theme app/src/main/java/io/github/kardeiro/gallery/MainActivity.kt app/build.gradle.kts gradle/libs.versions.toml`
> If any in-scope file changed since this plan was written, compare the "Current state" excerpts against the live code before proceeding; on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: MED
- **Depends on**: none
- **Category**: ui-ux
- **Planned at**: commit `400d821`, 2026-06-22

## Why this matters

The app already uses Material 3 and dynamic color, but the visual system is still close to default Material: plain surfaces, default dark fallback colors, and no shared expressive tokens for cards, viewer overlays, empty states, or motion. Before redesigning individual screens, establish a small Material You/M3 Expressive foundation so later plans can reuse consistent shape, spacing, elevation, and motion values. This keeps the Kardeiro principles of minimalism, privacy, and durability while making the app feel more polished and intentionally designed.

## Current state

Relevant files:

- `app/src/main/java/io/github/kardeiro/gallery/ui/theme/Theme.kt` — selects dynamic color on Android 12+ and a light fallback otherwise.
- `app/src/main/java/io/github/kardeiro/gallery/ui/theme/Color.kt` — defines only a limited light fallback palette.
- `app/src/main/java/io/github/kardeiro/gallery/ui/theme/Type.kt` — defines Material type roles with default-like sizing and weights.
- `app/src/main/java/io/github/kardeiro/gallery/MainActivity.kt` — applies `GalleryTheme` around the app.

Current theme excerpt:

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/theme/Theme.kt:13-27
private val LightColorScheme = lightColorScheme(
    primary = FallbackPrimary,
    onPrimary = FallbackOnPrimary,
    primaryContainer = FallbackPrimaryContainer,
    onPrimaryContainer = FallbackOnPrimaryContainer,
    secondary = FallbackSecondary,
    onSecondary = FallbackOnSecondary,
    secondaryContainer = FallbackSecondaryContainer,
    onSecondaryContainer = FallbackOnSecondaryContainer,
    tertiary = FallbackTertiary,
    background = FallbackBackground,
    onBackground = FallbackOnBackground,
    surface = FallbackSurface,
    onSurface = FallbackOnSurface,
)
```

```kotlin
// app/src/main/java/io/github/kardeiro/gallery/ui/theme/Color.kt:5-19
// Fallback colors when dynamic color is unavailable
val FallbackPrimary = Color(0xFF006D40)
val FallbackOnPrimary = Color(0xFFFFFFFF)
val FallbackPrimaryContainer = Color(0xFF95F7B5)
val FallbackOnPrimaryContainer = Color(0xFF002111)
...
val FallbackSurface = Color(0xFFFBFDF8)
val FallbackOnSurface = Color(0xFF191C1A)
```

Repo conventions to match:

- Keep design token code under `ui/theme`.
- Prefer Material 3 semantic tokens (`primary`, `surfaceContainer`, `surfaceVariant`, `error`, `outlineVariant`) over hardcoded colors in screens.
- Preserve dynamic color on Android 12+.
- Keep the app minimal; do not add external design-system libraries for this plan.

Material You/M3 Expressive direction for this app:

- Product type: private gallery, content-first media browsing.
- Style: expressive minimalism, soft organic corners, dynamic color, generous breathing room, strong hierarchy without heavy decoration.
- Accessibility priorities: 48dp touch targets, contrast-safe semantic tokens, dynamic type tolerance, reduced-motion-safe animation constants.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Build | `./gradlew assembleDebug` | exit 0 and `BUILD SUCCESSFUL` |
| Status | `git status --short` | only intended files changed |

## Scope

**In scope**:

- `app/src/main/java/io/github/kardeiro/gallery/ui/theme/Theme.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/theme/Color.kt`
- `app/src/main/java/io/github/kardeiro/gallery/ui/theme/Type.kt`
- Create `app/src/main/java/io/github/kardeiro/gallery/ui/theme/Shape.kt` if useful.
- Create `app/src/main/java/io/github/kardeiro/gallery/ui/theme/Spacing.kt` or `ExpressiveTokens.kt` if useful.
- `plans/README.md`

**Out of scope**:

- Redesigning screen layouts; that belongs to plans 021-023.
- Adding non-Material UI libraries.
- Removing dynamic color.
- Adding custom font assets unless the repo already contains them.
- Changing package name, navigation, data loading, or repository code.

## Git workflow

- Work on the current feature branch unless the operator instructed a different branch.
- Commit message suggestion: `autonomous-improve: add expressive Material 3 theme tokens`.
- Do not push unless the operator explicitly asked for it.

## Steps

### Step 1: Complete fallback light and dark color schemes

In `Color.kt`, keep existing fallback values but add a matching dark fallback palette and additional Material 3 roles used by expressive surfaces:

- `surfaceVariant`
- `onSurfaceVariant`
- `surfaceContainerLowest`
- `surfaceContainerLow`
- `surfaceContainer`
- `surfaceContainerHigh`
- `surfaceContainerHighest`
- `outline`
- `outlineVariant`
- `scrim`
- `error`, `onError`, `errorContainer`, `onErrorContainer`

Use calm green/teal fallback tones aligned with the existing Kardeiro palette. Do not use raw colors in screen files as part of this plan.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 2: Use explicit fallback dark scheme in Theme.kt

In `Theme.kt`, add a `DarkColorScheme = darkColorScheme(...)` using the dark fallback tokens. Keep dynamic color logic unchanged for Android 12+:

```kotlin
val colorScheme = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}
```

Also expand `LightColorScheme` to include the extra roles added in Step 1.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 3: Add expressive shape and spacing tokens

Create a small token file under `ui/theme`, for example `ExpressiveTokens.kt`, with composable-neutral constants such as:

```kotlin
object GallerySpacing {
    val Tiny = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 24.dp
    val Section = 32.dp
}

object GalleryCorner {
    val Thumbnail = 18.dp
    val Card = 24.dp
    val Sheet = 32.dp
}
```

If you add shape definitions, keep them compatible with Material 3 `Shapes` and use soft, expressive corners without making thumbnails look like unrelated blobs.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 4: Refine typography for expressive hierarchy

In `Type.kt`, keep Material role names but make the hierarchy slightly more intentional:

- Keep body text at 14-16sp minimum.
- Use `FontWeight.SemiBold` for prominent titles and screen headings.
- Do not reduce label sizes below current values.
- Preserve line heights so dynamic text scaling has room.

This is a subtle token-level adjustment only. Do not change text strings or screen layout.

**Verify**: `./gradlew assembleDebug` -> exits 0 and prints `BUILD SUCCESSFUL`.

### Step 5: Update the plan index

After the build passes, mark plan 020 as `DONE ✅` in `plans/README.md`.

**Verify**: `git status --short` -> only in-scope files are modified.

## Test plan

- Build verification: `./gradlew assembleDebug` -> `BUILD SUCCESSFUL`.
- Manual visual test on Android 12+ and pre-Android 12 if available: dynamic color should still follow wallpaper on Android 12+, and fallback colors should look coherent on older devices.
- Manual dark-mode test: force dark theme and verify text, icons, dialogs, and bottom/top bars remain readable.
- Manual accessibility test: increase font size in system settings and confirm titles/body text do not collapse in the existing screens.

## Done criteria

- [ ] `./gradlew assembleDebug` exits 0 and prints `BUILD SUCCESSFUL`.
- [ ] Dynamic color remains enabled on Android 12+.
- [ ] Non-dynamic light and dark color schemes are explicitly defined.
- [ ] Shared spacing/corner tokens exist under `ui/theme`.
- [ ] No screen layout files are modified except `plans/README.md`.
- [ ] `plans/README.md` marks plan 020 as `DONE ✅`.

## STOP conditions

Stop and report back if:

- The Material 3 version does not expose a color role you planned to use; adapt by using roles available in the current dependency, but do not upgrade dependencies in this plan.
- The change requires adding custom font files or a new dependency.
- Dynamic color must be removed to complete the plan.
- Build fails twice after reasonable import/token fixes.

## Maintenance notes

- Later layout plans should import and reuse these tokens instead of inventing per-screen `dp` values.
- Reviewers should check dark-mode contrast; expressive colors must not reduce readability.
- If future AndroidX Material3 versions add expressive APIs, migrate tokens gradually rather than rewriting all screens at once.
