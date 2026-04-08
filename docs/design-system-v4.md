# TabMates Design System

> **Source of truth:** Derived exclusively from [`mockup-requirements.md`](./mockup-requirements.md).
> **Last updated:** 2026-04-08
> **Status:** Token values defined — palette generated from seed `#A83200` (Deep Burnt Orange) using M3 Expressive high-chroma guidelines.

---

## Table of Contents

1. [Brand & Personality](#1-brand--personality)
2. [Brand Assets](#2-brand-assets)
3. [Color System](#3-color-system)
4. [Typography](#4-typography)
5. [Spacing & Grid](#5-spacing--grid)
6. [Elevation](#6-elevation)
7. [Shape](#7-shape)
8. [Iconography](#8-iconography)
9. [Illustration & Empty States](#9-illustration--empty-states)
10. [Motion & Animation](#10-motion--animation)
11. [Component States](#11-component-states)
12. [Accessibility](#12-accessibility)
13. [Localization](#13-localization)
14. [Open Decisions](#14-open-decisions)

---

## 1. Brand & Personality

### Tone

| Attribute | Direction |
|---|---|
| **Overall feel** | Warm & social — friend-group vibes |
| **Visual style** | Colorful, vibrant, playful, approachable |
| **Design language** | Material 3 Expressive — bold type, high-chroma palette, spring motion, generous shapes |
| **Reference apps** | Google Photos, Google Contacts, Android 16 system apps, Google app (Search) |
| **NOT** | Corporate, sterile, cold, flat/minimal, standard M3 without expressive enhancements |

The app should make an awkward topic (money between friends) feel casual and stress-free.

### Reference App Borrowings

| Reference App | What to borrow |
|---|---|
| **Google app (Search)** | Profile avatar in top-right, search bar as prominent hero element, rounded card surfaces |
| **Google Photos** | Spring-physics transitions, shared-element hero animations, vibrant use of all three palette roles |
| **Google Contacts** | Large initials/avatar heroes, generous use of container colors, FAB morphing |
| **Android 16 system apps** | Floating pill-style bottom nav bar, bold expressive headlines, high-chroma palette, springy transitions |

---

## 2. Brand Assets

> **Status: TBD — all assets to be created by the designer from scratch. No existing logo, wordmark, or brand identity exists.**

| Asset | Notes |
|---|---|
| **Wordmark / Logotype** | "TabMates" — designer to create |
| **App icon** | Adaptive icon (Android), standard icon (iOS/desktop) |
| **Favicon** | For web target |
| **Splash screen graphic** | If applicable |

---

## 3. Color System

### Design language

**Material 3 Expressive** — extends the baseline M3 palette with:
- **Higher chroma / higher saturation** — colors feel rich and energetic, not pastel or muted.
- **All three palette roles used actively** — primary (navigation, key CTAs), secondary (chips, tags, filter states), tertiary (balance indicators, accent highlights). Do **not** default to primary everywhere.
- **Container colors drive visual structure** — `primaryContainer`, `secondaryContainer`, `tertiaryContainer` used on cards, chips, and section headers.
- **Fixed colors for persistent brand moments** — `primaryFixed` family maintains color regardless of light/dark mode (illustration accents, onboarding spots, category pill backgrounds).
- **5-tier Surface Container hierarchy** — replaces `surfaceVariant` for visual depth without elevation shadows.
- Both **light and dark** mode variants required.
- **Android 12+ dynamic color** support — designed scheme is the fallback. `primaryFixed` tokens are immune to dynamic color override.

> **Generation tool:** Use [Material Theme Builder](https://material-foundation.github.io/material-theme-builder/) to generate all tokens from a chosen seed color.

### Seed Color

| Property | Value |
|---|---|
| **Seed** | `#A83200` (Deep Burnt Orange) |
| **Primary hue** | ~18° — deep red-orange (bold, warm, energetic) |
| **Secondary hue** | ~14° — terracotta / warm reddish-brown (earthy, approachable) |
| **Tertiary hue** | ~48° — golden-amber (rich; maps naturally to "positive" / money owed to you) |
| **Error hue** | Standard M3 red (~25°, high chroma) |

The seed was chosen to maintain the brand personality: **warm & social, vibrant, playful**. The three roles are visually distinct across all UI zones — deep red-orange CTAs, terracotta chips/tags, golden-amber balance highlights.

> ⚠️ **Note on tertiary shift:** In this palette the tertiary role has shifted from teal-green to golden-amber. The `positive` semantic token (user is owed money) now renders in golden-amber tones rather than green. Gold/amber carries strong financial-positive associations (banking, wealth, investment UI) and the `tertiaryContainer` (`#C8A932`) is uniquely **theme-invariant** — the same hex in both light and dark. The `secondary` (terracotta) role is also newly assigned a semantic meaning: `pending` states (unacknowledged expenses, unconfirmed settlements). See §3.5 for the full semantic model.

---

### 3.1 Core Role Tokens

| Token | Light Mode | Dark Mode | Primary Usage |
|---|---|---|---|
| `primary` | `#A83200` | `#FFB59D` | Key CTAs, active nav indicator, FAB |
| `onPrimary` | `#FFFFFF` | `#5D1800` | Text/icons on `primary` |
| `primaryContainer` | `#CB4A1A` | `#F06534` | Chips, selected cards, prominent surface zones |
| `onPrimaryContainer` | `#FFFBFF` | `#3B0C00` | Text/icons on `primaryContainer` |
| `secondary` | `#904B34` | `#FFB59D` | Tags, category chips, `pending` state (→ §3.5) |
| `onSecondary` | `#FFFFFF` | `#561F0B` | Text/icons on `secondary` |
| `secondaryContainer` | `#FEA588` | `#73341F` | `pendingContainer` — filter chips, unacknowledged-expense backgrounds |
| `onSecondaryContainer` | `#783923` | `#F79F83` | Text/icons on `secondaryContainer` |
| `tertiary` | `#715C00` | `#E5C44B` | Accent highlights, `positive` balance state (→ §3.5), special financial moments |
| `onTertiary` | `#FFFFFF` | `#3B2F00` | Text/icons on `tertiary` |
| `tertiaryContainer` | `#C8A932` | `#C8A932` | `positiveContainer` — golden background, **same hex in both themes** |
| `onTertiaryContainer` | `#4D3E00` | `#4D3E00` | Text/icons on `tertiaryContainer` — **same in both themes** |
| `error` | `#BA1A1A` | `#FFB4AB` | Validation errors, delete actions |
| `onError` | `#FFFFFF` | `#690005` | Text/icons on `error` |
| `errorContainer` | `#FFDAD6` | `#93000A` | Error banner backgrounds |
| `onErrorContainer` | `#93000A` | `#FFDAD6` | Text/icons on `errorContainer` |

---

### 3.2 Fixed Color Tokens _(light/dark-independent)_

Fixed colors maintain the **same value in both light and dark** themes. Use them for elements where consistent brand color matters regardless of mode.

| Token | Value | Usage |
|---|---|---|
| `primaryFixed` | `#CB4A1A` | Persistent primary-toned surface |
| `primaryFixedDim` | `#FFB59D` | Slightly dimmer fixed primary (hover/pressed on fixed surfaces) |
| `onPrimaryFixed` | `#FFFBFF` | Text/icons on `primaryFixed` |
| `onPrimaryFixedVariant` | `#FFFBFF` | Secondary text/icons on `primaryFixed` (lower emphasis) |
| `secondaryFixed` | `#FEA588` | Persistent secondary-toned surface |
| `secondaryFixedDim` | `#FFB59D` | Dimmer fixed secondary |
| `onSecondaryFixed` | `#783923` | Text/icons on `secondaryFixed` |
| `onSecondaryFixedVariant` | `#783923` | Secondary text/icons on `secondaryFixed` |
| `tertiaryFixed` | `#C8A932` | Persistent tertiary-toned surface |
| `tertiaryFixedDim` | `#E5C44B` | Dimmer fixed tertiary |
| `onTertiaryFixed` | `#4D3E00` | Text/icons on `tertiaryFixed` |
| `onTertiaryFixedVariant` | `#4D3E00` | Secondary text/icons on `tertiaryFixed` |

> **Note:** Fixed tokens are derived from the same tonal palette as their non-fixed counterparts (e.g., `primaryFixed` sits at the container tier of the primary tonal palette). Material Theme Builder generates these automatically.

---

### 3.3 Surface & Background Tokens

M3 Expressive uses a **5-tier Surface Container hierarchy** to layer cards, sheets, and list items without relying on elevation shadows.

| Token | Light Mode | Dark Mode | Usage |
|---|---|---|---|
| `background` | `#FFF8F6` | `#1C100D` | App background (behind all content) |
| `onBackground` | `#251914` | `#F6DDD6` | Text/icons on `background` |
| `surface` | `#FFF8F6` | `#1C100D` | Base surface (equals `background` in M3) |
| `onSurface` | `#251914` | `#F6DDD6` | Primary text/icons on `surface` |
| `surfaceVariant` | `#FEDBD1` | `#59413A` | Kept for backward compatibility; prefer container tiers below |
| `onSurfaceVariant` | `#59413A` | `#E1BFB5` | Secondary text/icons, subtitle text |
| `surfaceContainerLowest` | `#FFFFFF` | `#170B08` | Lowest-emphasis container (page background inset) |
| `surfaceContainerLow` | `#FFF1ED` | `#251914` | List item backgrounds, subtle grouping |
| `surfaceContainer` | `#FFE9E3` | `#2A1C18` | Standard card background |
| `surfaceContainerHigh` | `#FCE3DC` | `#352722` | Elevated cards, selected list items |
| `surfaceContainerHighest` | `#F6DDD6` | `#40312D` | Top-level modal surfaces, dialogs |
| `surfaceBright` | `#FFF8F6` | `#453631` | Highlighted surface areas (status bar zone, hero sections) |
| `surfaceDim` | `#EDD5CE` | `#1C100D` | Dimmed surface (scrim behind modal overlays) |
| `inverseSurface` | `#3C2D28` | `#F6DDD6` | Snackbar background |
| `inverseOnSurface` | `#FFEDE8` | `#3C2D28` | Snackbar text |
| `inversePrimary` | `#FFB59D` | `#AC3401` | FAB icon on dark surface |
| `surfaceTint` | `#A83200` | `#FFB59D` | Tonal elevation overlay color (= `primary`) |

---

### 3.4 Utility Tokens

| Token | Light Mode | Dark Mode | Usage |
|---|---|---|---|
| `outline` | `#8D7168` | `#A88A81` | Borders, dividers, text field outlines |
| `outlineVariant` | `#E1BFB5` | `#59413A` | Subtle dividers, chip outlines |
| `scrim` | `#000000` | `#000000` | Modal overlay scrim (typically black at ~32% opacity) |
| `shadow` | `#000000` | `#000000` | Drop shadow color (typically black) |

---

### 3.5 Semantic / App-Specific Tokens

Custom semantic tokens that **alias** M3 palette colors for expense-specific contexts. These are not new hues — they carry domain meaning.

All **four** M3 palette roles are mapped to expense-specific states, creating a complete semantic spectrum:

> **Semantic spectrum (visual progression):**
> `settled` (neutral warm cream) → `pending` (terracotta/in-motion) → `positive` (financial gold) ↔ `negative` (alert red)

#### Balance / Debt State Tokens

| Semantic Token | Maps to | Light Value | Dark Value | Usage |
|---|---|---|---|---|
| `positive` | `tertiary` | `#715C00` | `#E5C44B` | User is owed money — gold tones convey financial value; **always paired with an icon/label** |
| `positiveContainer` | `tertiaryContainer` | `#C8A932` ★ | `#C8A932` ★ | Background for "owed" balance cards |
| `onPositiveContainer` | `onTertiaryContainer` | `#4D3E00` | `#4D3E00` | Text/icons rendered on `positiveContainer` |
| `negative` | `error` | `#BA1A1A` | `#FFB4AB` | User owes money — **always paired with an icon/label** |
| `negativeContainer` | `errorContainer` | `#FFDAD6` | `#93000A` | Background for "owes" balance cards |
| `onNegativeContainer` | `onErrorContainer` | `#93000A` | `#FFDAD6` | Text/icons rendered on `negativeContainer` |
| `pending` | `secondary` | `#904B34` | `#FFB59D` | Expense not yet acknowledged, settlement unconfirmed, invite awaiting acceptance |
| `pendingContainer` | `secondaryContainer` | `#FEA588` | `#73341F` | Background for pending-state cards and banners |
| `onPendingContainer` | `onSecondaryContainer` | `#783923` | `#F79F83` | Text/icons rendered on `pendingContainer` |
| `settled` | `surfaceContainerHigh` / `onSurfaceVariant` | `#FCE3DC` bg, `#59413A` text | `#352722` bg, `#E1BFB5` text | Debt fully settled — deliberately muted, low visual weight |
| `deleted` | `onSurfaceVariant` @ 60% α | `#59413A` @ 60% | `#E1BFB5` @ 60% | Muted strikethrough treatment for deleted entries |

> **★ `positiveContainer` is theme-invariant:** `#C8A932` (golden amber) is the **same hex value in both light and dark** modes. This is a unique property of this palette — the "financial gold" background is a consistent visual anchor regardless of the user's theme. Leverage this: the positive balance card always reads as "gold."

> **Why golden-amber for `positive`?** In financial product design (banking apps, Bloomberg, investment dashboards), gold/amber signals wealth, value, and positive return — not warning. The warning/caution meaning of amber is a traffic-light convention that does not carry over to financial UIs when properly paired with directional iconography and text labels. **Do NOT use `tertiary`/`positive` tokens for generic UI warnings or caution states** — those belong to a future `warning` token or `error` at reduced prominence.

> **Why terracotta for `pending`?** The `secondary` role (terracotta/warm salmon `#FEA588`) occupies the middle ground between neutral (settled) and gold (positive). It reads as "in motion / not yet resolved" — warm and noticeable without being alarming. Every expense that has been added but not yet acknowledged by all participants should surface in this state.

> **Note on naming:** Canonical names are `positive`, `negative`, `pending`. The aliases `youAreOwed` / `youOwe` are acceptable as code-level synonyms but are not design token names.

> ⚠️ **Accessibility:** Color alone must never convey financial state. Always pair every state color with a directional icon (↑ ↓ ✓ ⏳) **and** a text label. This is especially critical for `positive` (amber) vs `pending` (terracotta) which are both warm and may be difficult to distinguish for some users with color vision deficiency.

---

## 4. Typography

### Platform Fonts

| Platform | Font |
|---|---|
| Android | **Roboto** (system default) |
| iOS | **SF Pro** (system default) |
| Web / Desktop | **`system-ui`** (platform system sans-serif) |

No custom or embedded typefaces. Use M3 Expressive defaults.

### Type Scale

| Style | Size | Weight | Line Height | Usage |
|---|---|---|---|---|
| Display Large | 57 sp | **700** | 64 sp | ★ Expressive hero moment only (e.g., onboarding splash) |
| Display Medium | 45 sp | **600** | 52 sp | ★ Large balance hero on Home screen |
| Display Small | 36 sp | **600** | 44 sp | ★ Settlement total, group balance callout |
| Headline Large | 32 sp | **700** | 40 sp | Screen titles, key section headers |
| Headline Medium | 28 sp | **600** | 36 sp | Section headers, Group Detail header |
| Headline Small | 24 sp | **600** | 32 sp | Card titles, dialog headers |
| Title Large | 22 sp | 500 | 28 sp | Top app bar |
| Title Medium | 16 sp | 500 | 24 sp | List item titles |
| Title Small | 14 sp | 500 | 20 sp | Sub-section labels |
| Body Large | 16 sp | 400 | 24 sp | Primary body text |
| Body Medium | 14 sp | 400 | 20 sp | Secondary text |
| Body Small | 12 sp | 400 | 16 sp | Captions, timestamps |
| Label Large | 14 sp | 500 | 20 sp | Buttons, tabs |
| Label Medium | 12 sp | 500 | 16 sp | Chips, badges |
| Label Small | 11 sp | 500 | 16 sp | Overlines |

> **★ Expressive moments:** Display styles are reserved for hero numbers and key emotional beats. The net balance amount on the Home screen hero card is the primary candidate for Display Medium. Avoid Display styles in lists or dense UI.
>
> **Weight note:** The heavier weights (600–700) at Display level are a deliberate M3 Expressive departure from standard M3's all-400 display scale.

---

## 5. Spacing & Grid

### Spacing Scale

| Token | Value |
|---|---|
| **Base unit** | 8 dp |
| **Minimum spacing** | 4 dp |
| **Common spacings** | 4, 8, 12, 16, 24, 32, 48, 64 dp |
| **Screen padding — mobile** | 16 dp horizontal |
| **Screen padding — tablet** | 24 dp horizontal |
| **Screen padding — desktop** | 24–32 dp horizontal (within panels) |
| **Card internal padding** | 16 dp |
| **List item height** | 56–72 dp (single / two-line) |

### Responsive Column Grid

| Breakpoint | Width | Columns | Gutter | Margin |
|---|---|---|---|---|
| **Mobile** | < 600 dp | 4 | 16 dp | 16 dp |
| **Tablet** | 600–1200 dp | 8 | 24 dp | 24 dp |
| **Desktop** | > 1200 dp | 12 | 24 dp | 24 dp |

> **Foldable devices (MVP):** Devices in the 600–840 dp range (e.g., Samsung Galaxy Z Fold unfolded) use the tablet layout as-is (nav rail + two panels). No dedicated foldable breakpoint for MVP.

---

## 6. Elevation

| Level | Elevation | Usage |
|---|---|---|
| Level 0 | 0 dp | Background surfaces |
| Level 1 | 1 dp | Cards, navigation rail |
| Level 2 | 3 dp | Top app bar (scrolled), FAB |
| Level 3 | 6 dp | Bottom sheets, dialogs |
| Level 4 | 8 dp | Menus, dropdowns |
| Level 5 | 12 dp | Popovers |

---

## 7. Shape

M3 Expressive favors **generous corner radii** — shapes should feel "soft" throughout, with hero cards using Extra Large or Full radii.

### Shape Scale

| Token | Radius | Usage |
|---|---|---|
| Extra Small | 4 dp | Badges, notification dots |
| Small | 8 dp | Small chips, text field corners |
| Medium | **16 dp** | Cards, list item containers |
| Large | **24 dp** | FAB, prominent buttons, dialogs |
| Extra Large | 28 dp | Bottom sheets, hero cards, large modal surfaces |
| Full | 50% | Avatar circles, toggle tracks, pill chips |

### Asymmetric Corners _(M3 Expressive option)_

For hero cards and feature highlight panels, consider asymmetric corner radii to create a recognizable shape language (e.g., top-left 28 dp, top-right 28 dp, bottom-left 8 dp, bottom-right 28 dp). This pattern appears in Android 16 overview cards and Google Contacts detail headers.

**Rule:** Apply asymmetric corners only to designated hero moments — Home balance card, Group Detail header. Do **not** apply randomly.

---

## 8. Iconography

| Attribute | Spec |
|---|---|
| **Library** | Material Symbols |
| **Style** | **Rounded** variant |
| **Weight** | 400 (default), 700 for emphasis |
| **Fill** | Filled for selected/active states; outlined for inactive |
| **Optical size** | 24 dp default, 20 dp for dense contexts |

> ⚠️ **No emojis.** All icons must use Material Symbols. Emojis are explicitly excluded.

---

## 9. Illustration & Empty States

- Use **friendly, simple illustrations** for empty states (no groups, no expenses, no activity).
- Style must match the **warm, playful** brand personality.
- Use brand palette colors in illustrations.
- Keep illustrations **lightweight** — not overly detailed or heavy.
- Illustrations are used on: onboarding pages, empty group list, empty expense list, empty activity feed, error screens.

---

## 10. Motion & Animation

### Principles

| Principle | Description |
|---|---|
| **Meaningful** | Motion communicates relationships (e.g., expanding a card reveals detail) |
| **Focused** | Draw attention to important changes (balance update, new expense) |
| **Expressive** | Spring-based curves with slight overshoot — playful, physical feel. Not linear or ease-in-out. |
| **Springy** | Spring physics (low stiffness, moderate damping) for sheet reveals, FAB morphs, card expansions |
| **Continuous** | Avoid abrupt cuts. Chain animations end-to-end (FAB morphs into bottom sheet rather than disappearing) |

### Spring Physics Reference

| Parameter | Value | Notes |
|---|---|---|
| **Stiffness** | 380 | Standard spring stiffness for most transitions |
| **Damping ratio** | 0.8 | Slightly underdamped — allows a very brief overshoot |
| **Duration cap** | 500 ms | Springs are duration-less by physics; cap at 500 ms for usability |
| **Enter duration** | ~350 ms | Screens/sheets entering |
| **Exit duration** | ~200 ms | Screens/sheets exiting (faster, less spring) |

### Key Animations

| Animation | Specification |
|---|---|
| **Screen transitions** | Shared element transitions where possible (e.g., group card → group detail). Predictive back gesture (Android 14+). |
| **Navigation tab switch** | Spring scale + crossfade on the active indicator pill — matches Android 16 nav bar behavior |
| **List item entry** | Staggered fade-in + slight upward slide on load (spring, 30 ms stagger per item) |
| **FAB** | Scale animation on appear; **morphs** into bottom sheet on tap (continuous spring transition) |
| **Profile avatar tap** | Scale pop (1.0 → 1.1 → 1.0 spring), then bottom sheet / popover slides in |
| **Balance changes** | Animated counter — number rolls to new value using a spring ticker |
| **Pull-to-refresh** | M3 standard refresh indicator |
| **Swipe to delete** | Slide + fade with undo snackbar (snackbar springs in from bottom) |
| **Bottom sheet** | Spring-based slide-up with scrim fade; slight overshoot allowed on open |
| **Skeleton loading** | Shimmer — left-to-right gradient sweep, 1200 ms loop |
| **Shared element hero** | Group card avatar / name morphs into Group Detail header (spring-driven shared element transition) |

---

## 11. Component States

### 11.1 Buttons _(Filled, Outlined, Text, FAB)_

| State | Visual Treatment |
|---|---|
| **Default** | Standard appearance |
| **Hover** | Slight elevation increase + state layer (8% opacity) |
| **Pressed** | State layer (12% opacity) + ripple |
| **Focused** | Focus ring (2 dp outline, primary color) |
| **Disabled** | 38% opacity, no interaction |
| **Loading** | Replace label with circular progress indicator; interaction disabled |

### 11.2 Text Fields

| State | Visual Treatment |
|---|---|
| **Default** | Outlined or filled variant, placeholder text |
| **Focused** | Border color → `primary`; label floats |
| **Filled** | Label remains floated, text visible |
| **Error** | Border color → `error`; supporting text shows error message |
| **Disabled** | Muted colors, no interaction |

### 11.3 Cards _(Group Card, Expense Card, Balance Card)_

| State | Visual Treatment |
|---|---|
| **Default** | Elevation Level 1 |
| **Hover** | Elevation Level 2 + subtle scale or highlight |
| **Pressed** | Elevation Level 0 + state layer |
| **Selected** | Outlined with `primary` color (list-detail views on tablet/desktop) |

### 11.4 List Items

| State | Visual Treatment |
|---|---|
| **Default** | Standard appearance |
| **Hover** | Background state layer |
| **Pressed** | Ripple |
| **Selected** | `primaryContainer` background |
| **Swipe left** | Delete action — `error` / red background |
| **Swipe right** | Edit / Settle action — contextual color |

> **Accessibility:** Swipe actions must have accessible alternatives (e.g., long-press menu).

### 11.5 Screen-Level States

Every data-loading screen must support all of the following:

| State | Treatment |
|---|---|
| **Loading** | Skeleton placeholders (shimmer) matching the shape of real content |
| **Empty** | Illustration + message + CTA (context-specific) |
| **Error** | Error illustration + message + "Retry" button |
| **Populated** | Normal content |
| **Pull-to-refresh** | M3 pull refresh indicator (mobile) |
| **Offline / syncing** | Subtle sync status indicator (small icon in top bar or dismissible banner). Must **never** block core actions. Optimistic UI — show result immediately, sync in background. |

### 11.6 App-Specific Components

#### BalanceCard

| State | Background | Amount Color | Icon | Label |
|---|---|---|---|---|
| **Positive** (user is owed) | `positiveContainer` — `#C8A932` both modes ★ | `positive` — `#715C00` / `#E5C44B` | ↑ arrow (filled) | "You are owed" |
| **Negative** (user owes) | `negativeContainer` — `#FFDAD6` / `#93000A` | `negative` — `#BA1A1A` / `#FFB4AB` | ↓ arrow (filled) | "You owe" |
| **Pending** (awaiting acknowledgement) | `pendingContainer` — `#FEA588` / `#73341F` | `pending` — `#904B34` / `#FFB59D` | ⏳ hourglass | "Awaiting response" |
| **Settled / zero** | `surfaceContainerHigh` — `#FCE3DC` / `#352722` | `onSurfaceVariant` — `#59413A` / `#E1BFB5` | ✓ checkmark | "All settled" |

> ★ `positiveContainer` (`#C8A932`) is identical in light and dark — the golden card is always "golden."
>
> Color is **always** paired with the icon AND text label. Never rely on color alone — `positive` (amber) and `pending` (warm salmon) are both warm-toned and must be distinguishable via shape, icon, and label.

#### GroupCard

| Element | Spec |
|---|---|
| Group name | Title Medium |
| Member avatars | Stacked initials circles, max 4 visible + "+N" overflow |
| Balance | Color-coded per BalanceCard rules |
| Last activity | Body Small, `onSurfaceVariant` |

#### ExpenseListItem

| Element | Spec |
|---|---|
| Title | Title Medium |
| Amount | Title Medium, end-aligned |
| Payer name | Body Small |
| Date | Body Small, `onSurfaceVariant` |
| Category icon | 20 dp Material Symbol (if categories enabled) |
| Deleted state | Strikethrough, `deleted` color, "deleted" badge |
| Edited state | Small pencil icon or "edited" label near timestamp |

#### CategoryChip

| Element | Spec |
|---|---|
| Icon | 20 dp Material Symbol (Rounded, outlined / filled for selected) |
| Label | Label Medium |
| Shape | Full (pill) |
| Background (unselected) | `secondaryContainer` (`#FEA588` / `#73341F`) |
| Background (selected) | `secondary` (`#904B34` / `#FFB59D`) with `onSecondary` text |

> **`secondaryContainer` dual role:** The same container color serves both CategoryChips and `pendingContainer` backgrounds. This is intentional — both represent "in-progress / not-yet-final" states. Ensure context (icon, label, card shape) disambiguates usage.

> Predefined category set: Food & Drink, Groceries, Transport, Accommodation, Entertainment, Shopping, Utilities, Health, Other. Custom categories deferred to post-MVP.

#### Avatar

| Variant | Spec |
|---|---|
| **Initials circle** (default) | Full shape (50%), background color derived from name hash using palette colors, initials in `onPrimary` or contrasting tone |
| **Photo** | Deferred to post-MVP |
| **Placeholder member** | Dashed-border circle, "Pending" badge, italic name |
| **Removed member** | Greyed-out, dashed border, italic `[Removed Member]` label |

#### SyncStatusIndicator

| State | Treatment |
|---|---|
| **Online / synced** | Hidden |
| **Syncing** | Small animated sync icon in top bar (subtle, non-blocking) |
| **Offline** | Small cloud-off icon or dismissible banner: "Offline — changes will sync when connected." Must never prevent app use. |

---

## 12. Accessibility

### 12.1 Color & Contrast

| Requirement | Specification |
|---|---|
| **Normal text** | ≥ 4.5 : 1 contrast ratio (WCAG AA) |
| **Large text** (≥ 18 sp or ≥ 14 sp bold) | ≥ 3 : 1 contrast ratio |
| **Interactive elements** | ≥ 3 : 1 against adjacent colors |
| **Never color alone** | Always pair color with icons, text labels, or patterns |

### 12.2 Touch Targets

| Requirement | Specification |
|---|---|
| **Minimum touch target** | 48 × 48 dp |
| **Recommended spacing** | 8 dp between targets |
| **Icon buttons** | Touch area 48 dp even if icon is 24 dp |

### 12.3 Screen Reader & Focus

| Requirement | Specification |
|---|---|
| **Content descriptions** | All images, icons, and decorative elements must have content descriptions or be marked as decorative |
| **Focus order** | Logical reading order: top-to-bottom, left-to-right |
| **Headings** | Proper heading hierarchy for screen readers |
| **Live regions** | Balance updates, error messages, and snackbars must be announced |
| **Custom actions** | Swipe actions must have accessible long-press alternatives |

---

## 13. Localization

| Requirement | Specification |
|---|---|
| **Languages** | Multi-language from launch; English is primary |
| **RTL** | Not at launch, but **all layouts must be RTL-ready**: use `start`/`end` instead of `left`/`right`; no hardcoded text positioning |
| **Text expansion** | Design with ~40% text expansion buffer. Buttons and labels must not truncate. |
| **Currency formatting** | Auto-detect from locale. Support: `€1.234,56` (DE), `$1,234.56` (US), `¥1,234` (JP), etc. |
| **Number formatting** | Locale-aware decimal separators and grouping |
| **Date formatting** | Locale-aware (DD/MM/YYYY vs MM/DD/YYYY). Use relative dates where possible ("2 hours ago", "Yesterday"). |
| **Time zones** | Store all timestamps in UTC; display in the **user's local device time zone**. A given timestamp may appear as "Today" for one user and "Yesterday" for another — this is acceptable (matches Slack / WhatsApp behavior). |
| **Pluralization** | Design must accommodate plural rules (some languages have more than 2 plural forms). Example: "1 expense" vs "2 expenses". |

---

## 14. Open Decisions

These items from `mockup-requirements.md §12` are unresolved and affect final component design. Recommendations are listed; decisions must be confirmed before finalizing mockups.

| # | Topic | Recommendation | Decision |
|---|---|---|---|
| 12.1 | **Custom expense categories** | Predefined set (9 categories) for MVP; custom categories post-MVP | ⬜ Pending |
| 12.2 | **Photo upload for avatars** | Initials-circle default for MVP; photo upload post-MVP | ⬜ Pending |
| 12.3 | **Notifications vs Activity** | Unified into single "Activity" tab; unread badge on tab | ⬜ Pending |
| 12.4 | **Donation screen prominence** | List item in Settings only; optional subtle Home card after 2+ weeks active | ⬜ Pending |
| 12.5 | **Partial settlement UX** | Option A (in-place update with remaining amount) for MVP; Option B (expandable history) as stretch goal | ⬜ Pending |
| 12.6 | **Global cross-group search** | Explicitly deferred to post-MVP; per-screen search only at launch | ⬜ Pending |
| 12.7 | **Session expiry re-auth** | Silent refresh preferred; fallback = bottom sheet re-auth overlay preserving in-progress state | ⬜ Pending |
| 12.8 | **Foldable devices** | Tablet layout as-is for MVP (no dedicated foldable breakpoint) | ⬜ Pending |
| 12.9 | **Time zone display** | User's local device time zone (UTC stored, local displayed) | ⬜ Pending |
| 12.10 | **Invite link lifetime** | 7-day expiry; any member can regenerate (revokes previous); share entry in Members tab + Group Settings | ⬜ Pending |

---

*End of document.*

