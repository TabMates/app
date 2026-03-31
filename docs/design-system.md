# TabMates — Design System

> **Purpose:** Complete design token specification for TabMates.
> Derived from the [Mockup Requirements](./mockup-requirements.md).
>
> **Last updated:** 2026-03-31
> **Status:** Draft — Reviewed (accessibility fixes applied)

---

## Table of Contents

1. [Brand Personality Recap](#1-brand-personality-recap)
2. [Color Palette](#2-color-palette)
3. [Typography Scale](#3-typography-scale)
4. [Spacing & Grid](#4-spacing--grid)
5. [Elevation & Shadow](#5-elevation--shadow)
6. [Shape Tokens](#6-shape-tokens)
7. [Iconography](#7-iconography)
8. [Component States Summary](#8-component-states-summary)

---

## 1. Brand Personality Recap

| Attribute | Direction |
|---|---|
| **Overall feel** | Warm & social — friend-group vibes |
| **Visual style** | Colorful, playful, approachable |
| **Design language** | Material 3 Expressive |
| **NOT** | Corporate, sterile, cold, overly minimal |

**Color Story:** The palette is built around a **warm coral** primary — energetic and social without being aggressive. It's paired with an **ocean teal** secondary for trust and calm (subtly evoking "balance"), and a **soft violet** tertiary for playful richness. Together they feel like a group of friends, not a finance app.

---

## 2. Color Palette

### 2.1 Seed Colors

These are the three source hues from which the full tonal palettes are derived:

| Role | Seed Color | Hex | Preview |
|---|---|---|---|
| **Primary** | Warm Coral | `#C06040` | 🟧 |
| **Secondary** | Ocean Teal | `#3A8E80` | 🟩 |
| **Tertiary** | Soft Violet | `#7B6BA5` | 🟪 |
| **Error** | Material Red | `#BA1A1A` | 🟥 |
| **Neutral** | Warm Gray | `#201A17` | ⬛ |

### 2.2 Full Material 3 Token Table

#### Light Mode

| Token | Hex | RGB | Usage |
|---|---|---|---|
| `primary` | `#B05530` | 176, 85, 48 | Primary buttons, FAB, active indicators |
| `onPrimary` | `#FFFFFF` | 255, 255, 255 | Text/icons on primary |
| `primaryContainer` | `#FFDBD0` | 255, 219, 208 | Primary tonal fill (cards, chips) |
| `onPrimaryContainer` | `#3C0A00` | 60, 10, 0 | Text/icons on primary container |
| `secondary` | `#2E6F63` | 46, 111, 99 | Secondary buttons, toggles |
| `onSecondary` | `#FFFFFF` | 255, 255, 255 | Text/icons on secondary |
| `secondaryContainer` | `#B4F1E4` | 180, 241, 228 | Teal tonal fill (balance cards, "owed" chips) |
| `onSecondaryContainer` | `#00201A` | 0, 32, 26 | Text/icons on secondary container |
| `tertiary` | `#655790` | 101, 87, 144 | Accent elements, category badges |
| `onTertiary` | `#FFFFFF` | 255, 255, 255 | Text/icons on tertiary |
| `tertiaryContainer` | `#EBDDFF` | 235, 221, 255 | Tertiary tonal fill |
| `onTertiaryContainer` | `#211548` | 33, 21, 72 | Text/icons on tertiary container |
| `error` | `#BA1A1A` | 186, 26, 26 | Error states, destructive actions |
| `onError` | `#FFFFFF` | 255, 255, 255 | Text/icons on error |
| `errorContainer` | `#FFDAD6` | 255, 218, 214 | Error tonal fill |
| `onErrorContainer` | `#410002` | 65, 0, 2 | Text/icons on error container |
| `background` | `#FFFBFF` | 255, 251, 255 | Page background |
| `onBackground` | `#201A17` | 32, 26, 23 | Primary text on background |
| `surface` | `#FFFBFF` | 255, 251, 255 | Card/sheet surfaces |
| `onSurface` | `#201A17` | 32, 26, 23 | Primary text on surface |
| `surfaceVariant` | `#F5DDD5` | 245, 221, 213 | Muted surface (e.g., input fills) |
| `onSurfaceVariant` | `#53433D` | 83, 67, 61 | Secondary text on surface variant |
| `outline` | `#85736C` | 133, 115, 108 | Borders, dividers |
| `outlineVariant` | `#D8C2BA` | 216, 194, 186 | Subtle dividers |
| `surfaceDim` | `#E3D7D3` | 227, 215, 211 | Dimmed surface (e.g., scrim-adjacent) |
| `surfaceBright` | `#FFFBFF` | 255, 251, 255 | Brightest surface level |
| `surfaceContainerLowest` | `#FFFFFF` | 255, 255, 255 | Lowest-emphasis container |
| `surfaceContainerLow` | `#F9EDE9` | 249, 237, 233 | Low-emphasis container |
| `surfaceContainer` | `#F3E7E3` | 243, 231, 227 | Default container (nav bar, bottom sheets) |
| `surfaceContainerHigh` | `#EDE1DD` | 237, 225, 221 | High-emphasis container (search bar, dialogs) |
| `surfaceContainerHighest` | `#E7DBD7` | 231, 219, 215 | Highest-emphasis container (text fields) |
| `inverseSurface` | `#362F2C` | 54, 47, 44 | Snackbar background |
| `inverseOnSurface` | `#FBEEEA` | 251, 238, 234 | Snackbar text |
| `inversePrimary` | `#FFB59C` | 255, 181, 156 | Primary in inverse context |
| `surfaceTint` | `#B05530` | 176, 85, 48 | Tint overlay for elevation |
| `scrim` | `#000000` | 0, 0, 0 | Modal scrim (32% opacity) |

#### Dark Mode

| Token | Hex | RGB | Usage |
|---|---|---|---|
| `primary` | `#FFB59C` | 255, 181, 156 | Primary buttons, FAB, active indicators |
| `onPrimary` | `#612000` | 97, 32, 0 | Text/icons on primary |
| `primaryContainer` | `#893A18` | 137, 58, 24 | Primary tonal fill |
| `onPrimaryContainer` | `#FFDBD0` | 255, 219, 208 | Text/icons on primary container |
| `secondary` | `#98D5C8` | 152, 213, 200 | Secondary buttons, toggles |
| `onSecondary` | `#003830` | 0, 56, 48 | Text/icons on secondary |
| `secondaryContainer` | `#1A5049` | 26, 80, 73 | Teal tonal fill |
| `onSecondaryContainer` | `#B4F1E4` | 180, 241, 228 | Text/icons on secondary container |
| `tertiary` | `#D0BFFF` | 208, 191, 255 | Accent elements |
| `onTertiary` | `#36295E` | 54, 41, 94 | Text/icons on tertiary |
| `tertiaryContainer` | `#4D4077` | 77, 64, 119 | Tertiary tonal fill |
| `onTertiaryContainer` | `#EBDDFF` | 235, 221, 255 | Text/icons on tertiary container |
| `error` | `#FFB4AB` | 255, 180, 171 | Error states |
| `onError` | `#690005` | 105, 0, 5 | Text/icons on error |
| `errorContainer` | `#93000A` | 147, 0, 10 | Error tonal fill |
| `onErrorContainer` | `#FFDAD6` | 255, 218, 214 | Text/icons on error container |
| `background` | `#201A17` | 32, 26, 23 | Page background |
| `onBackground` | `#ECE0DC` | 236, 224, 220 | Primary text |
| `surface` | `#201A17` | 32, 26, 23 | Card/sheet surfaces |
| `onSurface` | `#ECE0DC` | 236, 224, 220 | Primary text |
| `surfaceVariant` | `#53433D` | 83, 67, 61 | Muted surface |
| `onSurfaceVariant` | `#D8C2BA` | 216, 194, 186 | Secondary text |
| `outline` | `#A08D85` | 160, 141, 133 | Borders, dividers |
| `outlineVariant` | `#53433D` | 83, 67, 61 | Subtle dividers |
| `surfaceDim` | `#201A17` | 32, 26, 23 | Dimmed surface |
| `surfaceBright` | `#483F3B` | 72, 63, 59 | Brightest surface level |
| `surfaceContainerLowest` | `#1B1512` | 27, 21, 18 | Lowest-emphasis container |
| `surfaceContainerLow` | `#292320` | 41, 35, 32 | Low-emphasis container |
| `surfaceContainer` | `#2D2724` | 45, 39, 36 | Default container (nav bar, bottom sheets) |
| `surfaceContainerHigh` | `#38322F` | 56, 50, 47 | High-emphasis container (search bar, dialogs) |
| `surfaceContainerHighest` | `#433D39` | 67, 61, 57 | Highest-emphasis container (text fields) |
| `inverseSurface` | `#ECE0DC` | 236, 224, 220 | Snackbar background |
| `inverseOnSurface` | `#362F2C` | 54, 47, 44 | Snackbar text |
| `inversePrimary` | `#B05530` | 176, 85, 48 | Primary in inverse context |
| `surfaceTint` | `#FFB59C` | 255, 181, 156 | Tint overlay for elevation |
| `scrim` | `#000000` | 0, 0, 0 | Modal scrim |

### 2.3 Semantic / Contextual Colors

These are **app-specific** tokens layered on top of M3 for expense-related UI and text hierarchy:

#### Light Mode

| Token | Hex | RGB | Usage |
|---|---|---|---|
| `positive` | `#2E7D32` | 46, 125, 50 | "You are owed" text, positive balance |
| `positiveContainer` | `#C8E6C9` | 200, 230, 201 | "You are owed" card/chip fill |
| `onPositiveContainer` | `#1B5E20` | 27, 94, 32 | Text on positive container |
| `negative` | `#C62828` | 198, 40, 40 | "You owe" text, negative balance |
| `negativeContainer` | `#FFCDD2` | 255, 205, 210 | "You owe" card/chip fill |
| `onNegativeContainer` | `#B71C1C` | 183, 28, 28 | Text on negative container |
| `settled` | `#78685F` | 120, 104, 95 | Settled debts, neutral state (warm taupe) |
| `settledContainer` | `#EDE4E0` | 237, 228, 224 | Settled card/chip fill |
| `onSettledContainer` | `#5C4F48` | 92, 79, 72 | Text on settled container |
| `deleted` | `#6B6B6B` | 107, 107, 107 | Deleted entries (base color; rendered at 60% opacity with strikethrough) |
| `deletedContainer` | `#F5F0EE` | 245, 240, 238 | Deleted entry background (warm-tinted) |
| `textSecondary` | `#53433D` | 83, 67, 61 | Subtitles, descriptions, secondary body copy (≈ 9.4:1) |
| `textTertiary` | `#85736C` | 133, 115, 108 | Timestamps, captions, metadata (≈ 4.6:1) |
| `textPlaceholder` | `#A08D85` | 160, 141, 133 | Placeholder / hint text in inputs (≈ 3.1:1, non-essential) |
| `textDisabled` | `#C4B5AD` | 196, 181, 173 | Disabled text (≈ 2.0:1, WCAG exempt for inactive UI) |
| `secondaryFill` | `#F0E0D9` | 240, 224, 217 | Secondary card fill, grouped section background |

#### Dark Mode

| Token | Hex | RGB | Usage |
|---|---|---|---|
| `positive` | `#81C784` | 129, 199, 132 | "You are owed" text |
| `positiveContainer` | `#1B5E20` | 27, 94, 32 | "You are owed" card/chip fill |
| `onPositiveContainer` | `#C8E6C9` | 200, 230, 201 | Text on positive container |
| `negative` | `#EF9A9A` | 239, 154, 154 | "You owe" text |
| `negativeContainer` | `#B71C1C` | 183, 28, 28 | "You owe" card/chip fill |
| `onNegativeContainer` | `#FFCDD2` | 255, 205, 210 | Text on negative container |
| `settled` | `#C4B5AD` | 196, 181, 173 | Settled debts (warm silver) |
| `settledContainer` | `#4A3F39` | 74, 63, 57 | Settled card/chip fill |
| `onSettledContainer` | `#E3D8D2` | 227, 216, 210 | Text on settled container |
| `deleted` | `#9E9E9E` | 158, 158, 158 | Deleted entries (base color; rendered at 60% opacity with strikethrough) |
| `deletedContainer` | `#3A3330` | 58, 51, 48 | Deleted entry background (warm-tinted) |
| `textSecondary` | `#D8C2BA` | 216, 194, 186 | Subtitles, descriptions, secondary body copy (≈ 9.0:1) |
| `textTertiary` | `#A08D85` | 160, 141, 133 | Timestamps, captions, metadata (≈ 4.8:1) |
| `textPlaceholder` | `#85736C` | 133, 115, 108 | Placeholder / hint text in inputs (≈ 3.4:1, non-essential) |
| `textDisabled` | `#5C4F48` | 92, 79, 72 | Disabled text (≈ 1.9:1, WCAG exempt for inactive UI) |
| `secondaryFill` | `#352D29` | 53, 45, 41 | Secondary card fill, grouped section background |

> **Text hierarchy (light):** `onSurface` 15.8:1 → `textSecondary` 9.4:1 → `textTertiary` 4.6:1 → `textPlaceholder` 3.1:1 → `textDisabled` 2.0:1
>
> **Text hierarchy (dark):** `onSurface` 12.4:1 → `textSecondary` 9.0:1 → `textTertiary` 4.8:1 → `textPlaceholder` 3.4:1 → `textDisabled` 1.9:1
>
> `textPlaceholder` deliberately falls below 4.5:1 — placeholder text is instructional and non-essential per WCAG. `textDisabled` is exempt as it represents inactive UI controls.

### 2.4 Color Usage Guidelines

| Context | Light Mode Approach | Dark Mode Approach |
|---|---|---|
| **Balance "You are owed €45"** | `positive` text + `positiveContainer` bg | `positive` text + `positiveContainer` bg |
| **Balance "You owe €12"** | `negative` text + `negativeContainer` bg | `negative` text + `negativeContainer` bg |
| **"All settled up! 🎉"** | `settled` text + `settledContainer` bg | `settled` text + `settledContainer` bg |
| **Deleted expense in list** | `deleted` text, strikethrough, 60% opacity | `deleted` text, strikethrough, 60% opacity |
| **FAB "Add Expense"** | `primary` bg + `onPrimary` icon/text | `primary` bg + `onPrimary` icon/text |
| **Group card** | `surface` bg, `onSurface` title, `onSurfaceVariant` subtitle | Same tokens, dark values |
| **Active nav item** | `primaryContainer` indicator + `onPrimaryContainer` icon | Same tokens, dark values |
| **Guest banner** | `tertiaryContainer` bg + `onTertiaryContainer` text | Same tokens, dark values |

### 2.5 Accessibility Notes

| Pair | Light Contrast | Dark Contrast | Verdict |
|---|---|---|---|
| `onPrimary` on `primary` | ≈ 5.0:1 | ≈ 5.3:1 | ✅ AA |
| `onPrimaryContainer` on `primaryContainer` | ≈ 17.5:1 | ≈ 7.1:1 | ✅ AA |
| `onSecondaryContainer` on `secondaryContainer` | ≈ 14.8:1 | ≈ 8.6:1 | ✅ AA |
| `onTertiaryContainer` on `tertiaryContainer` | ≈ 11.9:1 | ≈ 6.5:1 | ✅ AA |
| `onSurface` on `surface` | ≈ 15.8:1 | ≈ 12.4:1 | ✅ AA |
| `onSurfaceVariant` on `surfaceVariant` | ≈ 5.6:1 | ≈ 5.6:1 | ✅ AA |
| `positive` on `background` | ≈ 5.3:1 | ≈ 8.3:1 | ✅ AA |
| `negative` on `background` | ≈ 5.7:1 | ≈ 7.2:1 | ✅ AA |
| `onNegativeContainer` on `negativeContainer` | ≈ 4.7:1 | ≈ 4.7:1 | ✅ AA (borderline) |
| `settled` on `background` | ≈ 5.4:1 | ≈ 8.0:1 | ✅ AA |
| `onSettledContainer` on `settledContainer` | ≈ 6.4:1 | ≈ 6.8:1 | ✅ AA |
| `deleted` (base, full opacity) on `background` | ≈ 5.3:1 | ≈ 5.2:1 | ✅ AA at full opacity |
| `textSecondary` on `surface` | ≈ 9.4:1 | ≈ 9.0:1 | ✅ AA |
| `textTertiary` on `surface` | ≈ 4.6:1 | ≈ 4.8:1 | ✅ AA |
| `textPlaceholder` on `surface` | ≈ 3.1:1 | ≈ 3.4:1 | ⚠️ Non-essential hint text |
| `textDisabled` on `surface` | ≈ 2.0:1 | ≈ 1.9:1 | ℹ️ WCAG exempt (inactive UI) |

> **Note on `deleted` at 60% opacity:** The 60% opacity treatment is intentionally de-emphasized per design (WCAG exempts "incidental" or decorative text). The strikethrough styling and "Deleted" label/badge provide redundant non-color cues. The base color passes AA at full opacity for assistive technology and high-contrast modes.

---

## 3. Typography Scale

Platform fonts are used (Roboto on Android, SF Pro on iOS, system sans-serif on Web/Desktop). The scale follows M3 with **bolder treatments** for key financial figures (M3 Expressive direction).

| Style | Size | Weight | Line Height | Letter Spacing | Usage |
|---|---|---|---|---|---|
| **Display Large** | 57 sp | 400 | 64 sp | −0.25 sp | — |
| **Display Medium** | 45 sp | 400 | 52 sp | 0 sp | — |
| **Display Small** | 36 sp | 400 | 44 sp | 0 sp | — |
| **Headline Large** | 32 sp | 400 | 40 sp | 0 sp | Screen titles |
| **Headline Medium** | 28 sp | 400 | 36 sp | 0 sp | Section headers |
| **Headline Small** | 24 sp | 400 | 32 sp | 0 sp | Card titles |
| **Title Large** | 22 sp | 400 | 28 sp | 0 sp | Top app bar title |
| **Title Medium** | 16 sp | 500 | 24 sp | 0.15 sp | List item titles, expense names |
| **Title Small** | 14 sp | 500 | 20 sp | 0.1 sp | — |
| **Body Large** | 16 sp | 400 | 24 sp | 0.5 sp | Primary body text, descriptions |
| **Body Medium** | 14 sp | 400 | 20 sp | 0.25 sp | Secondary text, subtitles |
| **Body Small** | 12 sp | 400 | 16 sp | 0.4 sp | Captions, timestamps, "2h ago" |
| **Label Large** | 14 sp | 500 | 20 sp | 0.1 sp | Buttons, tabs, nav labels |
| **Label Medium** | 12 sp | 500 | 16 sp | 0.5 sp | Chips, badges, category labels |
| **Label Small** | 11 sp | 500 | 16 sp | 0.5 sp | Overlines, "EDITED" tag |

### Expressive Overrides (Finance-Specific)

| Custom Style | Base | Override | Usage |
|---|---|---|---|
| **Balance Amount** | Headline Large | **700 weight** | Net balance on Dashboard ("€45.20") |
| **Expense Amount** | Title Large | **600 weight** | Amount in expense list items |
| **Settlement Amount** | Headline Medium | **600 weight** | Settlement summary ("You owe Sarah €23.50") |
| **Currency Symbol** | Inherited | **400 weight**, 80% size | Slightly smaller currency prefix/suffix |

---

## 4. Spacing & Grid

### 4.1 Spacing Scale

| Token | Value (dp) | Common Usage |
|---|---|---|
| `space-xxs` | 4 | Inline icon-to-text gap, badge padding |
| `space-xs` | 8 | Chip internal padding, tight list gaps |
| `space-sm` | 12 | Between related elements within a card |
| `space-md` | 16 | Screen horizontal margin (mobile), card padding, standard gap |
| `space-lg` | 24 | Screen horizontal margin (tablet/desktop), section spacing |
| `space-xl` | 32 | Major section breaks |
| `space-xxl` | 48 | Hero card top/bottom padding |
| `space-xxxl` | 64 | Onboarding illustration spacing |

### 4.2 Screen Padding

| Context | Horizontal Padding |
|---|---|
| Mobile (< 600 dp) | 16 dp |
| Tablet (600–1200 dp) | 24 dp |
| Desktop (> 1200 dp) | 24–32 dp (within panels) |

### 4.3 Responsive Column Grid

| Breakpoint | Width | Columns | Gutter | Margin |
|---|---|---|---|---|
| **Compact** (Mobile) | < 600 dp | 4 | 16 dp | 16 dp |
| **Medium** (Tablet) | 600–1200 dp | 8 | 24 dp | 24 dp |
| **Expanded** (Desktop) | > 1200 dp | 12 | 24 dp | 24 dp |

### 4.4 Common Component Heights

| Component | Height |
|---|---|
| Top app bar (small) | 64 dp |
| Top app bar (medium/large) | 112–152 dp |
| Bottom navigation bar | 80 dp |
| Navigation rail width | 80 dp |
| Sidebar nav width | 240 dp |
| List item (single-line) | 56 dp |
| List item (two-line) | 72 dp |
| List item (three-line) | 88 dp |
| FAB | 56 dp (standard), 96 dp (large) |
| Mini FAB | 40 dp |
| Chip | 32 dp |
| Text field | 56 dp |
| Button | 40 dp |

---

## 5. Elevation & Shadow

| Level | Elevation | Shadow | Usage |
|---|---|---|---|
| **Level 0** | 0 dp | None | Background, flat cards |
| **Level 1** | 1 dp | `0 1dp 2dp rgba(0,0,0,0.15)` | Filled cards, navigation rail |
| **Level 2** | 3 dp | `0 1dp 3dp rgba(0,0,0,0.15), 0 1dp 2dp rgba(0,0,0,0.30)` | Scrolled top app bar, FAB (resting) |
| **Level 3** | 6 dp | `0 3dp 5dp rgba(0,0,0,0.15), 0 1dp 3dp rgba(0,0,0,0.30)` | Bottom sheets, dialogs |
| **Level 4** | 8 dp | `0 4dp 6dp rgba(0,0,0,0.15), 0 2dp 4dp rgba(0,0,0,0.30)` | Menus, dropdowns |
| **Level 5** | 12 dp | `0 6dp 10dp rgba(0,0,0,0.15), 0 2dp 6dp rgba(0,0,0,0.30)` | Popovers |

> **Note:** In M3, dark mode uses **surface tint** (surfaceTint at varying opacities) rather than shadow to convey elevation.

---

## 6. Shape Tokens

M3 Expressive uses generous corner radii for a playful, approachable feel:

| Token | Radius | Usage |
|---|---|---|
| **Extra Small** | 4 dp | Badges, small indicators |
| **Small** | 8 dp | Chips, text fields, small buttons |
| **Medium** | 12 dp | Cards, list items, expense items |
| **Large** | 16 dp | FAB, dialogs, large buttons |
| **Extra Large** | 28 dp | Bottom sheets, balance summary card, modals |
| **Full** | 50% (circular) | Avatar circles, toggle thumb, nav indicator pill |

### Shape Usage Map

| Component | Shape Token |
|---|---|
| Expense list item card | Medium (12 dp) |
| Group card | Medium (12 dp) |
| Balance summary card (dashboard) | Extra Large (28 dp) |
| FAB | Large (16 dp) |
| Filled button | Full (50%, pill shape) |
| Outlined button | Full (50%, pill shape) |
| Text field | Small (8 dp) |
| Chip (category, filter) | Small (8 dp) |
| Dialog | Extra Large (28 dp) |
| Bottom sheet | Extra Large (28 dp) top corners only |
| Avatar | Full (circular) |
| Navigation bar indicator | Full (pill) |
| Snackbar | Small (8 dp) |
| Badge | Extra Small (4 dp) |

---

## 7. Iconography

| Attribute | Specification |
|---|---|
| **Icon set** | Material Symbols — **Rounded** variant |
| **Default weight** | 400 |
| **Emphasis weight** | 700 |
| **Fill** | Filled (`FILL=1`) for selected/active states; Outlined (`FILL=0`) for inactive |
| **Default optical size** | 24 dp |
| **Dense optical size** | 20 dp (inside chips, dense lists) |
| **Touch target** | Always 48 × 48 dp minimum, even if icon is 24 dp |

### Key Icons

| Context | Icon Name | Notes |
|---|---|---|
| Home nav | `home` | Filled when active |
| Groups nav | `group` | Filled when active |
| Activity nav | `notifications` | Filled when active |
| Profile nav | `person` | Filled when active |
| Add Expense (FAB) | `add` | Always filled |
| Settings | `settings` | |
| Search | `search` | |
| Filter | `filter_list` | |
| Edit | `edit` | |
| Delete | `delete` | |
| Back | `arrow_back` | |
| Close | `close` | |
| Settle up | `handshake` | |
| Invite | `person_add` | |
| Share link | `link` | |
| Calendar / Date | `calendar_today` | |
| Currency | `payments` | |
| Positive balance | `arrow_downward` | Combined with positive color |
| Negative balance | `arrow_upward` | Combined with negative color |
| Category: Food | `restaurant` | |
| Category: Groceries | `shopping_cart` | |
| Category: Transport | `directions_car` | |
| Category: Accommodation | `hotel` | |
| Category: Entertainment | `celebration` | |
| Category: Shopping | `shopping_bag` | |
| Category: Utilities | `bolt` | |
| Category: Health | `medical_services` | |
| Category: Other | `category` | |
| Guest banner | `info` | |
| Donation / Support | `favorite` | |

---

## 8. Component States Summary

A quick-reference of state treatments across all interactive components:

### Buttons (Filled, Outlined, Text, FAB)

| State | Treatment |
|---|---|
| Default | Standard colors |
| Hover | +8% state layer (onPrimary/onSurface) |
| Pressed | +12% state layer + ripple |
| Focused | 2 dp focus ring in primary color |
| Disabled | 38% opacity, non-interactive |
| Loading | Circular progress replaces label, disabled |

### Text Fields

| State | Treatment |
|---|---|
| Default | Outline in `outline` color, placeholder text |
| Focused | Outline changes to `primary`, label floats, 2 dp border |
| Filled | Floated label, value visible |
| Error | Outline changes to `error`, supporting error text below |
| Disabled | 38% opacity |

### Cards (Group, Expense, Balance)

| State | Treatment |
|---|---|
| Default | Level 1 elevation |
| Hover | Level 2 elevation |
| Pressed | Level 0 + 12% state layer |
| Selected | 2 dp `primary` outline (tablet/desktop list-detail) |

### List Items

| State | Treatment |
|---|---|
| Default | Transparent background |
| Hover | 8% state layer |
| Pressed | Ripple |
| Selected | `primaryContainer` background |

### Screen-Level States

| State | Treatment |
|---|---|
| Loading | Skeleton shimmer (left-to-right gradient, 1.5s loop) |
| Empty | Illustration + message + CTA |
| Error | Error illustration + message + "Retry" button |
| Populated | Normal content |

---

## Appendix: Color Palette Visual Preview

### Light Mode Swatches

```
PRIMARY           ██████  #B05530   Warm Coral
ON PRIMARY        ██████  #FFFFFF   White
PRIMARY CONT.     ██████  #FFDBD0   Peach Cream
ON PRIMARY CONT.  ██████  #3C0A00   Deep Brown

SECONDARY         ██████  #2E6F63   Ocean Teal
ON SECONDARY      ██████  #FFFFFF   White
SECONDARY CONT.   ██████  #B4F1E4   Mint Foam
ON SECONDARY C.   ██████  #00201A   Dark Teal

TERTIARY          ██████  #655790   Soft Violet
ON TERTIARY       ██████  #FFFFFF   White
TERTIARY CONT.    ██████  #EBDDFF   Lavender Mist
ON TERTIARY C.    ██████  #211548   Deep Purple

ERROR             ██████  #BA1A1A   Red
ERROR CONTAINER   ██████  #FFDAD6   Pink

BACKGROUND        ██████  #FFFBFF   Off-White
SURFACE           ██████  #FFFBFF   Off-White
SURFACE VARIANT   ██████  #F5DDD5   Warm Blush
OUTLINE           ██████  #85736C   Warm Gray

POSITIVE          ██████  #2E7D32   Money Green
NEGATIVE          ██████  #C62828   Owe Red
SETTLED           ██████  #78685F   Warm Taupe
```

### Dark Mode Swatches

```
PRIMARY           ██████  #FFB59C   Soft Peach
ON PRIMARY        ██████  #612000   Dark Amber
PRIMARY CONT.     ██████  #893A18   Burnt Sienna
ON PRIMARY CONT.  ██████  #FFDBD0   Peach Cream

SECONDARY         ██████  #98D5C8   Seafoam
ON SECONDARY      ██████  #003830   Deep Teal
SECONDARY CONT.   ██████  #1A5049   Dark Teal
ON SECONDARY C.   ██████  #B4F1E4   Mint Foam

TERTIARY          ██████  #D0BFFF   Light Lavender
ON TERTIARY       ██████  #36295E   Dark Indigo
TERTIARY CONT.    ██████  #4D4077   Muted Violet
ON TERTIARY C.    ██████  #EBDDFF   Lavender Mist

BACKGROUND        ██████  #201A17   Warm Charcoal
SURFACE           ██████  #201A17   Warm Charcoal
SURFACE VARIANT   ██████  #53433D   Warm Brown
OUTLINE           ██████  #A08D85   Taupe

POSITIVE          ██████  #81C784   Soft Green
NEGATIVE          ██████  #EF9A9A   Soft Red
SETTLED           ██████  #C4B5AD   Warm Silver
```

---

*End of design system draft. Ready for review.*

