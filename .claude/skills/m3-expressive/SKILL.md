---
name: m3-expressive
description: Material 3 Expressive doctrine for spoo-android — version pinning, theming pattern, motion rules, component idioms, chart approach, and anti-patterns distilled from an exhaustive Aug 2026 census of ~55 real M3E open-source apps. Use for ANY UI work in this repo.
---

# M3 Expressive doctrine (spoo-android)

Distilled 2026-08-20 from a symbol census of every MD3E+FOSS app in
nyas1/Material-You-app-list (65 repos clone-inspected). Full catalog with
per-topic exemplars: `~/spoo/analysis/m3e-oss-catalog-2026-08.md`.

## Version doctrine
- Expressive exists ONLY in `material3` **1.5.0-alpha** (we pin alpha26,
  over the BOM). Nobody in the ecosystem ships it from stable 1.4.0. The
  headline APIs graduated within the alphas — churn risk is renames, not
  redesigns. Read release notes before ANY version bump; never bump material3
  as a side effect of a BOM bump.
- Compose BOM manages everything else. Nav3 is the navigation library; any
  `navigation-compose`/NavHost pattern in a search result is legacy Nav2 —
  do not import it.

## Theming
- Root is `MaterialExpressiveTheme(motionScheme = MotionScheme.expressive())`.
  (The census's alternative — expressive components on classic MaterialTheme
  for brand control — is a deliberate rejected-for-now option; revisit only
  if dynamic color fights the spoo identity.)
- Dynamic color (`dynamicLight/DarkColorScheme`) on API 31+; below that a
  seed-derived scheme (MaterialKolor, `PaletteStyle.Expressive`) — never a
  hand-picked hex palette.
- Ship and maintain the `monochrome` adaptive-icon layer; Android force-themes
  icons since 16 QPR2 and auto-generates an ugly one if absent.

## Motion rules
- Springs, not durations: **spatial** springs (position/size/shape) may
  bounce; **effects** springs (color/opacity) never bounce. Pull both from
  `MaterialTheme.motionScheme` — hardcoded `tween()`s are a review flag.
- Motion serves hierarchy, not decoration. Google's own Drive walked back an
  over-expressive redesign; the best census apps use ONE bold idiom well.

## The one-bold-idiom rule (structural)
Census finding: the best apps commit to a single structural signature —
flexible-app-bar-first (InstallerX, tokn), FloatingToolbar-as-nav
(WatchMaster), or ButtonGroup-as-controls (Reef, CaffeineHealth).
**spoo-android's chosen idiom: flexible app bars** (`LargeFlexibleTopAppBar`
scroll-collapse) as the structural signature; toolbars/button-groups appear
where they earn it, not as identity.

## Components — when to reach for what
- `LoadingIndicator` (morphing shapes) for waits <5s; wavy progress
  (`LinearWavyProgressIndicator`) only where progress IS the content
  (stats loading, playback-like states) — wavy everywhere reads as noise.
- `ButtonGroup` for connected mode/filter switches (not for unrelated actions).
- `SplitButton` for primary-action-plus-variants (e.g. Copy / copy as QR).
- `FloatingActionButtonMenu` only if the screen truly has 3+ creation verbs.
- Shape system: `MaterialShapes` presets first; raw `graphics.shapes`
  `Morph()` is our distinctiveness budget — reserved for link-state identity
  (à la RivoPhoneApp's avatar morphing), used in ONE place, done well.

## Charts
- Hand-rolled Canvas/`Path` is the norm (census: ONE Vico user in ~55 apps).
  The hero clicks-over-time wavy chart is custom: cubic Béziers through
  points, `PathMeasure` draw-on animation, gradient fill; wave math ref:
  `mahozad/wavy-slider`. Refs: Cashiro `BalanceChart.kt`, traffic-light bar
  graphs, Outify `WavyMusicSlider`.
- Vico (`compose-m3` artifact) only for genuinely generic breakdowns, themed
  from MaterialTheme — never with its default styling.

## Widgets (Glance)
- Glance 1.2.0-rc01 + `glance-material3`, `GlanceTheme` wired to our scheme.
  Widgets carry the expressive look too (refs: pennywiseai suite, Minus
  heatmaps, Zenith streaks, PixelPlayer's wavy-in-widget).
- Cache last data in DataStore per `GlanceId`; render stale data + timestamp,
  never spinners. Push-update after in-app mutations; 15-min WorkManager
  floor otherwise. Provide `providePreview` generated previews.

## Platform table stakes (targetSdk 36)
Edge-to-edge always (no opt-out); predictive back (Nav3 + material3 handle
it — never intercept KEYCODE_BACK); no orientation locking; test adaptive
icon in all masks.

## Anti-patterns (review flags)
- material3 bumped implicitly or unpinned
- Nav2 imports; XML layouts; Fragments; AppCompat themes
- `tween()`/hardcoded easing where a motion-scheme spring belongs
- wavy indicators as generic spinners; ButtonGroup as a link list
- chips/dots/status-badge chrome for state a quieter affordance could carry
  (NOTE: the zingzy-design-taste skill does NOT apply to this repo — zingzy's
  ruling Aug 2026: it is web-only; this app is pure M3 Expressive, don't
  pollute it. Never load that skill for spoo-android work.)
- Vico default styling; charts with library watermark aesthetics
