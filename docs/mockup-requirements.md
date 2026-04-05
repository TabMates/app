# TabMates — Mockup Requirements Document

> **Purpose:** Self-contained design brief for producing high-fidelity mockups.
> Designers should be able to work from this document alone.
>
> **Last updated:** 2026-04-05
> **Status:** Ready for design

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [Target Users & Core Scenarios](#2-target-users--core-scenarios)
3. [Brand & Visual Identity](#3-brand--visual-identity)
4. [Design System Specification](#4-design-system-specification)
5. [Information Architecture & Navigation](#5-information-architecture--navigation)
6. [Screen Inventory & Per-Screen Specs](#6-screen-inventory--per-screen-specs)
7. [Responsive Layout Rules](#7-responsive-layout-rules)
8. [Component States](#8-component-states)
9. [Accessibility Requirements](#9-accessibility-requirements)
10. [Motion & Animation](#10-motion--animation)
11. [Localization Considerations](#11-localization-considerations)
12. [Open Items for Designer Decision](#12-open-items-for-designer-decision)

---

## 1. Product Overview

| Field | Value |
|---|---|
| **App Name** | TabMates |
| **Category** | Expense-splitting / shared finance tracker |
| **Comparable Apps** | Splitwise, Split |
| **Platforms** | Android, iOS, Web (WASM), Desktop |
| **Tech Framework** | Kotlin Multiplatform + Compose Multiplatform |
| **Design Language** | Material Design 3 Expressive |
| **Monetization** | Free & open source — no paywalls, no premium tiers |
| **Current State** | Early scaffolding — no screens implemented yet |

### What TabMates Does

TabMates helps groups of people track shared expenses and settle debts. Users create groups, log expenses, define how costs are split, and the app calculates who owes whom. There is **no payment integration** — it is purely a tracking and calculation tool.

### What TabMates Does NOT Do (MVP)

- No receipt scanning / OCR
- No payment provider integration (PayPal, Venmo, etc.)
- No premium/paid features
- No RTL language support (but layouts must be RTL-ready)

---

## 2. Target Users & Core Scenarios

### Primary Audience

| Segment | Context |
|---|---|
| **University students** | Splitting rent, groceries, utilities |
| **Travel groups** | Trip expenses (flights, hotels, meals, activities) |
| **Couples / housemates** | Managing recurring household bills |

### Key User Characteristics

- Age range: ~18–35 (skews young)
- Tech-savvy, mobile-first
- Often in a hurry when logging expenses (e.g., at a restaurant)
- May be cost-conscious — the app itself must feel free and lightweight

### Hero Scenario: "Settling a Trip"

This is the #1 use case that must feel **effortless**. The typical flow:

1. One person creates a group and invites 3–6 friends via link
2. Throughout the trip, various people log expenses (meals, transport, accommodation)
3. Each expense is split among relevant members (not always everyone)
4. At the end of the trip, the app shows simplified debts ("You owe Sarah €23.50")
5. People settle up outside the app and mark debts as resolved

### Typical Group Size

- **Common:** 3–6 members
- **Supported:** No hard limit — must gracefully handle 10+ members (scrollable lists, overflow handling)

---

## 3. Brand & Visual Identity

### Brand Creation Required

There is **no existing logo, wordmark, or brand identity**. The designer must create these from scratch.

### Deliverables

- **Wordmark / Logotype** for "TabMates"
- **App icon** (adaptive icon for Android, standard icon for iOS/desktop)
- **Favicon** for web
- **Splash screen** graphic (if applicable)

### Reference Design Direction

TabMates should feel and behave like the **latest generation of Google's own first-party apps** — specifically:

| Reference App | What to borrow |
|---|---|
| **Google app (Search)** | Profile avatar in top-right, search bar as a prominent hero element, rounded card surfaces |
| **Google Photos** | Spring-physics transitions, shared-element hero animations, vibrant use of all three palette roles across different UI zones |
| **Google Contacts** | Large initials/avatar heroes, generous use of container colors to differentiate card types, FAB morphing |
| **Android 16 system apps** | Floating pill-style bottom nav bar, bold expressive headlines, high-chroma palette approach, springy page transitions |

The overarching standard is **Material 3 Expressive** as shipped in Android 16 and the 2025–2026 Google app refreshes. This is not standard M3 — it is the evolution that features bolder type, higher color saturation, spring physics, and more dramatic use of shape.

### Personality & Tone

| Attribute | Direction |
|---|---|
| **Overall feel** | Warm & social — friend-group vibes |
| **Visual style** | Colorful, vibrant, playful, approachable |
| **Design language** | Material 3 Expressive — bold type, high-chroma palette, spring motion, generous shapes |
| **Reference** | Google Photos, Google Contacts, Android 16 system apps |
| **NOT** | Corporate, sterile, cold, flat/minimal, standard M3 without expressive enhancements |

The app should feel like a friendly tool that makes an awkward topic (money between friends) feel casual and stress-free.

### Color Direction

**The full palette is open for redesign.** The designer has creative freedom to propose a new palette that matches the warm & social personality.

**Key M3 Expressive requirements for the palette:**

- **High chroma / high saturation** — M3 Expressive deliberately raises chroma above standard M3 defaults. Colors should feel rich and energetic, not pastel or muted.
- **All three palette roles must be used actively** — primary, secondary, and tertiary should each appear prominently across different UI zones (e.g., primary on navigation and key CTAs; secondary on chips, tags, and surface containers; tertiary on balance indicators and highlights).
- **Container colors drive visual structure** — use `primaryContainer`, `secondaryContainer`, `tertiaryContainer` on cards, chips, and section headers, not just as subtle accents.
- **Fixed colors for persistent brand moments** — `primaryFixed` and its variants are used for elements that must maintain color regardless of light/dark mode (e.g., category pill backgrounds in illustrations, onboarding highlight spots).
- Both light and dark mode variants
- Android 12+ dynamic color support (the theme must work as a fallback when dynamic color is unavailable)

---

## 4. Design System Specification

### 4.1 Color Palette

Deliver a **complete Material 3 Expressive color scheme** covering all token groups. M3 Expressive extends the baseline M3 palette with Fixed colors, a 5-tier Surface Container hierarchy, and surfaceBright/Dim variants. All tokens below must be defined for both light and dark mode.

#### Core Role Tokens

| Token | Light Mode | Dark Mode | Primary Usage |
|---|---|---|---|
| `primary` | TBD | TBD | Key CTAs, active nav indicator, FAB |
| `onPrimary` | TBD | TBD | Text/icons on primary |
| `primaryContainer` | TBD | TBD | Chips, selected cards, prominent surface zones |
| `onPrimaryContainer` | TBD | TBD | Text/icons on primaryContainer |
| `secondary` | TBD | TBD | Tags, category indicators, secondary actions |
| `onSecondary` | TBD | TBD | Text/icons on secondary |
| `secondaryContainer` | TBD | TBD | Filter chips, balance indicator backgrounds |
| `onSecondaryContainer` | TBD | TBD | Text/icons on secondaryContainer |
| `tertiary` | TBD | TBD | Accent highlights, settlement badges, special moments |
| `onTertiary` | TBD | TBD | Text/icons on tertiary |
| `tertiaryContainer` | TBD | TBD | Stat callout cards, decorative highlights |
| `onTertiaryContainer` | TBD | TBD | Text/icons on tertiaryContainer |
| `error` | TBD | TBD | Validation errors, delete actions |
| `onError` | TBD | TBD | Text/icons on error |
| `errorContainer` | TBD | TBD | Error banner backgrounds |
| `onErrorContainer` | TBD | TBD | Text/icons on errorContainer |

#### Fixed Color Tokens (light/dark-independent)

Fixed colors maintain the same value in both light and dark themes. Use them for elements where consistent brand color matters regardless of mode (e.g., illustration accents, onboarding spots, category pill backgrounds).

| Token | Value | Usage |
|---|---|---|
| `primaryFixed` | TBD | Persistent primary-toned surface |
| `primaryFixedDim` | TBD | Slightly dimmer fixed primary (for hover/pressed states on fixed) |
| `onPrimaryFixed` | TBD | Text/icons on primaryFixed |
| `onPrimaryFixedVariant` | TBD | Secondary text/icons on primaryFixed (lower emphasis) |
| `secondaryFixed` | TBD | Persistent secondary-toned surface |
| `secondaryFixedDim` | TBD | Dimmer fixed secondary |
| `onSecondaryFixed` | TBD | Text/icons on secondaryFixed |
| `onSecondaryFixedVariant` | TBD | Secondary text/icons on secondaryFixed |
| `tertiaryFixed` | TBD | Persistent tertiary-toned surface |
| `tertiaryFixedDim` | TBD | Dimmer fixed tertiary |
| `onTertiaryFixed` | TBD | Text/icons on tertiaryFixed |
| `onTertiaryFixedVariant` | TBD | Secondary text/icons on tertiaryFixed |

#### Surface & Background Tokens

M3 Expressive replaces the single `surfaceVariant` with a **5-tier Surface Container hierarchy** to create visual depth without relying on elevation shadows. Use the tiers to layer cards, sheets, and list items.

| Token | Light Mode | Dark Mode | Usage |
|---|---|---|---|
| `background` | TBD | TBD | App background (behind all content) |
| `onBackground` | TBD | TBD | Text/icons on background |
| `surface` | TBD | TBD | Base surface (same as background in M3) |
| `onSurface` | TBD | TBD | Primary text/icons on surface |
| `surfaceVariant` | TBD | TBD | Kept for backward compatibility; prefer container tiers below |
| `onSurfaceVariant` | TBD | TBD | Secondary text/icons, subtitle text |
| `surfaceContainerLowest` | TBD | TBD | Lowest-emphasis container (e.g., page background inset) |
| `surfaceContainerLow` | TBD | TBD | List item backgrounds, subtle grouping |
| `surfaceContainer` | TBD | TBD | Standard card background |
| `surfaceContainerHigh` | TBD | TBD | Elevated cards, selected list items |
| `surfaceContainerHighest` | TBD | TBD | Top-level modal surfaces, dialogs |
| `surfaceBright` | TBD | TBD | Highlighted surface areas (status bar zone, hero sections) |
| `surfaceDim` | TBD | TBD | Dimmed surface (scrim behind modal overlays) |
| `inverseSurface` | TBD | TBD | Snackbar background |
| `inverseOnSurface` | TBD | TBD | Snackbar text |
| `inversePrimary` | TBD | TBD | FAB icon on dark surface |
| `surfaceTint` | TBD | TBD | Tonal elevation overlay color (= primary) |

#### Utility Tokens

| Token | Light Mode | Dark Mode | Usage |
|---|---|---|---|
| `outline` | TBD | TBD | Borders, dividers, text field outlines |
| `outlineVariant` | TBD | TBD | Subtle dividers, chip outlines |
| `scrim` | TBD | TBD | Modal overlay scrim (typically black at ~32% opacity) |
| `shadow` | TBD | TBD | Drop shadow color (typically black) |

#### Semantic / App-Specific Colors

Define **custom semantic tokens** that map onto M3 palette colors for expense-specific contexts. These are not new hues — they are aliases into the palette that carry domain meaning.

| Semantic Token | Maps to | Usage |
|---|---|---|
| `positive` / `youAreOwed` | `tertiary` or custom green tone | User is owed money — always paired with an icon/label, never color alone |
| `positiveContainer` | `tertiaryContainer` | Background for "owed" balance cards |
| `negative` / `youOwe` | `error` or custom red tone | User owes money — always paired with an icon/label |
| `negativeContainer` | `errorContainer` | Background for "owes" balance cards |
| `settled` | `surfaceContainerHigh` / `onSurfaceVariant` | Debt fully settled — muted/neutral treatment |
| `deleted` | `onSurfaceVariant` at reduced opacity | Muted strikethrough treatment for deleted entries |

> **Designer note:** All three palette roles (primary, secondary, tertiary) must appear visibly and distinctly across the app. Do not default to using only primary everywhere. Reference how Google Photos uses tertiary for highlight moments and secondary for chip states.

### 4.2 Typography Scale

Follow the **M3 Expressive type scale**. The app uses the platform default font on each platform:

| Platform | Font |
|---|---|
| Android | Roboto / system default |
| iOS | SF Pro |
| Web / Desktop | System sans-serif stack |

Define sizes for:

| Style | Size | Weight | Line Height | Usage |
|---|---|---|---|---|
| Display Large | 57sp | **700** | 64sp | ★ Expressive hero moment only (e.g., onboarding splash) |
| Display Medium | 45sp | **600** | 52sp | ★ Large balance hero on Home screen |
| Display Small | 36sp | **600** | 44sp | ★ Settlement total, group balance callout |
| Headline Large | 32sp | **700** | 40sp | Screen titles, key section headers |
| Headline Medium | 28sp | **600** | 36sp | Section headers, Group Detail header |
| Headline Small | 24sp | **600** | 32sp | Card titles, dialog headers |
| Title Large | 22sp | 500 | 28sp | Top app bar |
| Title Medium | 16sp | 500 | 24sp | List item titles |
| Title Small | 14sp | 500 | 20sp | Sub-section labels |
| Body Large | 16sp | 400 | 24sp | Primary body text |
| Body Medium | 14sp | 400 | 20sp | Secondary text |
| Body Small | 12sp | 400 | 16sp | Captions, timestamps |
| Label Large | 14sp | 500 | 20sp | Buttons, tabs |
| Label Medium | 12sp | 500 | 16sp | Chips, badges |
| Label Small | 11sp | 500 | 16sp | Overlines |

> **★ Expressive moments:** Display styles are reserved for hero numbers and key emotional beats. In M3 Expressive, these are intentionally bold and large — they should feel impactful. The balance amount on the Home screen hero card is the primary candidate for Display Medium. Avoid using Display styles in lists or dense UI. The heavier weights (600–700) are a deliberate departure from standard M3's all-400 display scale.

### 4.3 Spacing & Grid

| Token | Value |
|---|---|
| **Base unit** | 8 dp |
| **Minimum spacing** | 4 dp |
| **Common spacings** | 4, 8, 12, 16, 24, 32, 48, 64 dp |
| **Screen padding (mobile)** | 16 dp horizontal |
| **Screen padding (tablet)** | 24 dp horizontal |
| **Screen padding (desktop)** | 24–32 dp horizontal (within panels) |
| **Card internal padding** | 16 dp |
| **List item height** | 56–72 dp (single/two-line) |

**Responsive column grid:**

| Breakpoint | Columns | Gutter | Margin |
|---|---|---|---|
| Mobile (< 600 dp) | 4 | 16 dp | 16 dp |
| Tablet (600–1200 dp) | 8 | 24 dp | 24 dp |
| Desktop (> 1200 dp) | 12 | 24 dp | 24 dp |

### 4.4 Elevation & Shadow

Follow M3 elevation levels:

| Level | Elevation | Usage |
|---|---|---|
| Level 0 | 0 dp | Background surfaces |
| Level 1 | 1 dp | Cards, navigation rail |
| Level 2 | 3 dp | Top app bar (scrolled), FAB |
| Level 3 | 6 dp | Bottom sheets, dialogs |
| Level 4 | 8 dp | Menus, dropdowns |
| Level 5 | 12 dp | Popovers |

### 4.5 Shape Tokens

M3 Expressive favors **generous corner radii** that are noticeably more rounded than standard M3. As a reference, Google Contacts and Android 16 system apps use shapes that feel "soft" throughout, with hero cards using Extra Large or Full radii.

| Token | Radius | Usage |
|---|---|---|
| Extra Small | 4 dp | Badges, notification dots |
| Small | 8 dp | Small chips, text field corners |
| Medium | **16 dp** | Cards, list item containers |
| Large | **24 dp** | FAB, prominent buttons, dialogs |
| Extra Large | 28 dp | Bottom sheets, hero cards, large modal surfaces |
| Full | 50% | Avatar circles, toggle tracks, pill chips |

**Asymmetric corners (M3 Expressive option):** For hero cards and feature highlight panels, consider using asymmetric corner radii (e.g., top-left 28 dp, top-right 28 dp, bottom-left 8 dp, bottom-right 28 dp) to create a unique, recognizable shape language. This is an expressive pattern used in Android 16 overview cards and Google Contacts detail headers. Propose a specific usage pattern if desired — do not apply randomly.

### 4.6 Iconography

| Attribute | Direction |
|---|---|
| **Style** | Material Symbols (Rounded variant) |
| **Weight** | 400 (default), 700 for emphasis |
| **Fill** | Filled for selected/active states, outlined for inactive |
| **Optical size** | 24 dp default, 20 dp for dense contexts |

### 4.7 Illustration & Empty States

- Use friendly, simple illustrations for empty states (no groups yet, no expenses, etc.)
- Style should match the warm/playful brand personality
- Consider using the brand colors in illustrations
- Keep illustrations lightweight — not overly detailed or heavy

---

## 5. Information Architecture & Navigation

### 5.1 Navigation Map

```
TabMates App
│
├── Onboarding (first launch only)
│   └── Welcome / Feature highlights → Sign In or Continue as Guest
│
├── Authentication
│   ├── Sign In (email/password)
│   ├── Sign Up (email/password)
│   ├── Forgot Password
│   ├── Confirmed Email (after clicking link)
│   └── Guest Mode (skip auth,  no way of signing in on the same account on another device, and data is lost after clearing app data)
│
├── ★ Home / Dashboard (default landing)
│   ├── Balance summary (total owed / total owing)
│   ├── Recent activity feed (across all groups)
│   └── Quick actions (create group, add expense)
│
├── ★ Groups
│   ├── Group List (all user's groups)
│   ├── Group Detail
│   │   ├── Expense List (with filters / search)
│   │   ├── Balances Tab (who owes whom within this group)
│   │   ├── Members Tab (member list, invite actions)
│   │   └── Settings (group name, default currency, leave/delete)
│   ├── Create Group
│   ├── Join Group (via invite link)
│   └── Add/Edit Expense
│       ├── Amount entry
│       ├── Payer selection
│       ├── Split method selection
│       ├── Split detail / adjustment
│       ├── Currency selection (defaults to group currency)
│       ├── Date picker
│       ├── Notes / description
│       └── Category selection
│
├── ★ Activity
│   ├── Unified activity feed (all events across all groups)
│   │   ├── Expense created / edited / deleted events
│   │   ├── Member joined / left events
│   │   ├── Settlement events
│   │   └── Group created events
│   └── Push notification history
│
├── ◎ Profile (accessed via avatar in top app bar — not a bottom nav item)
│   ├── User info (name, email)
│   ├── Account settings
│   │   ├── Change password
│   │   ├── Switch to full account (if started as guest)
│   │   └── Delete account
│   ├── App settings
│   │   ├── Default currency
│   │   ├── Theme (light / dark / system)
│   │   ├── Notification preferences
│   │   └── Language
│   ├── Support the Developers (donation prompt)
│   ├── Feedback / Rate the App
│   └── About / Licenses
│
└── Settlement Flow
    ├── Debt summary (simplified: "You owe X to Y")
    ├── Mark as settled (confirmation dialog)
    └── Settlement history
```

> **★** = Top-level navigation destination (mobile bottom bar / tablet rail / desktop sidebar)
> **◎** = Accessed globally via the **profile avatar button in the top app bar**, present on all main screens. Tapping the avatar opens a quick-action menu (View Profile, Settings, Sign Out) from which the full Profile screen is reached. This follows the same pattern as the Google app, Gmail, and Google Photos.

### 5.2 Navigation Patterns by Breakpoint

| Breakpoint | Navigation | Profile Access | Detail Behavior |
|---|---|---|---|
| **Mobile** (< 600 dp) | Bottom navigation bar — **3 items: Home, Groups, Activity** | Profile avatar in top app bar (top-right) → bottom sheet quick menu | Full-screen push navigation |
| **Tablet** (600–1200 dp) | Navigation rail (left side) — **3 items: Home, Groups, Activity** | Profile avatar in top app bar (top-right) → popover quick menu | Two-panel: list + detail side by side |
| **Desktop** (> 1200 dp) | Persistent sidebar — **3 items: Home, Groups, Activity** + avatar at bottom of sidebar | Profile avatar at bottom of sidebar AND in top bar → dropdown quick menu | Three-panel: sidebar + list + detail |

**Profile avatar quick menu** (appears on avatar tap across all breakpoints):

```
┌─────────────────────────┐
│ [Avatar]  Name          │
│           email@...     │
├─────────────────────────┤
│ ○  View Profile         │
│ ⚙  Settings             │
├─────────────────────────┤
│ ⎋  Sign Out             │
└─────────────────────────┘
```

- On **mobile**: rendered as a bottom sheet (spring slide-up)
- On **tablet/desktop**: rendered as a dropdown popover anchored to the avatar

### 5.3 iOS-Specific Navigation

On iOS, use **native navigation components** (UINavigationController-style transitions, large title collapsing headers) and **SF Pro typography** to match platform conventions. The visual design (colors, shapes, illustrations) stays unified.

---

## 6. Screen Inventory & Per-Screen Specs

### 6.1 Onboarding (First Launch)

**Purpose:** Introduce the app and guide toward first action.

**Layout:** 1–3 screen carousel or single welcome screen.

| Element | Specification |
|---|---|
| **Illustration** | Friendly, on-brand illustration per page |
| **Headline** | Short, punchy value proposition (e.g., "Split expenses, not friendships") |
| **Body text** | 1–2 sentence description |
| **Primary CTA** | "Get Started" → navigates to Sign Up |
| **Secondary CTA** | "I already have an account" → Sign In |
| **Tertiary link** | "Continue without account" → Guest mode → Home |
| **Skip** | If carousel, allow skip to last page |

**States:**
- Only shown on first launch (or after logout/uninstall)
- Page indicator dots if carousel

---

### 6.2 Authentication Screens

#### 6.2.1 Sign Up

| Element | Specification |
|---|---|
| **Fields** | Name, Email, Password, Confirm Password |
| **Validation** | Inline field-level errors (email format, password strength, match) |
| **Primary CTA** | "Create Account" |
| **Secondary** | "Already have an account? Sign In" |
| **Tertiary** | "Continue as Guest" |

#### 6.2.2 Sign In

| Element | Specification |
|---|---|
| **Fields** | Email, Password |
| **Primary CTA** | "Sign In" |
| **Secondary** | "Don't have an account? Sign Up" |
| **Tertiary** | "Forgot Password?" |
| **Tertiary** | "Continue as Guest" |

#### 6.2.3 Forgot Password

| Element | Specification |
|---|---|
| **Fields** | Email |
| **Primary CTA** | "Send Reset Link" |
| **Success state** | Confirmation message: "Check your inbox" |
| **Back** | Return to Sign In |

#### 6.2.4 Guest Mode

- No separate screen — user taps "Continue without account" and goes directly to Home
- A **persistent but non-intrusive banner** on the Home screen reminds guest users: "Create an account to sync your data across devices"
- Guest users have full functionality locally; data is device-only

**States for all auth screens:**
- Default (empty form)
- Filling (active input)
- Validation error (inline per-field + summary)
- Loading (button shows spinner, fields disabled)
- Server error (snackbar or inline message)

---

### 6.3 Home / Dashboard

**Purpose:** At-a-glance summary of the user's financial state and recent activity.

| Element | Specification |
|---|---|
| **Top app bar** | "TabMates" wordmark or logo (start/left), notification bell icon, **profile avatar circle** (end/right). Tapping the avatar opens the profile quick-action menu. Balance summary card uses Display Medium (bold) for the amount — this is a key expressive hero moment. |
| **Balance summary card** | Large, prominent hero card using `primaryContainer` or `positiveContainer`/`negativeContainer` background. Shows: net balance ("You are owed €45.20" or "You owe €12.00") in Display Medium weight, color-coded (positive = `tertiary` tones, negative = `error` tones, settled = `surfaceContainerHigh`). |
| **Group list preview** | Horizontal scrollable row or vertical list of 2–3 most active groups with name, member count, and user's balance in that group |
| **"See all groups" link** | Navigates to Groups tab |
| **Recent activity feed** | Last 5–10 events across all groups (expense added, settled, member joined) with timestamp, group name, and brief description |
| **FAB** | "Add Expense" (primary action) — opens a sheet/dialog to select group first, then the expense creation flow |
| **Empty state** | When user has no groups: illustration + "Create your first group" CTA + "Join a group" secondary CTA |

**States:**
- Empty (no groups)
- Populated (groups with balances)
- Loading (skeleton placeholders)
- Error (network error, retry button)
- Guest mode (with account creation banner)

---

### 6.4 Groups

#### 6.4.1 Group List

**Purpose:** Browse and manage all groups the user belongs to.

| Element | Specification |
|---|---|
| **Top app bar** | Title: "Groups" (Headline Large, bold), search icon, **profile avatar circle** (end/right) |
| **Group cards** | Each card shows: group name, member avatars (stacked circles, max 4 visible + "+N"), user's balance in that group (color-coded), last activity timestamp |
| **Sorting** | Default: most recently active first |
| **Create group CTA** | FAB or prominent button: "New Group" |
| **Empty state** | Illustration + "No groups yet" + "Create Group" / "Join Group" CTAs |
| **Search** | Filter groups by name |

#### 6.4.2 Create Group

**Purpose:** Create a new expense-sharing group.

| Element | Specification |
|---|---|
| **Fields** | Group name (required), Group description (optional), Default currency (picker, defaults to user's locale currency), Group avatar/icon (optional — see Open Items) |
| **Add members** | Invite by email field (can add multiple), generate shareable invite link |
| **Placeholder members** | Ability to add a name as a "placeholder" for someone not yet registered — this reserves a spot that a real user can claim later via invite link |
| **Primary CTA** | "Create Group" |
| **After creation** | Navigate to the new Group Detail screen |

#### 6.4.3 Group Detail

**Purpose:** Central hub for a group — view expenses, balances, and members.

**Layout:** Top section with group header, then tabbed content below.

| Element | Specification |
|---|---|
| **Group header** | Group name, member count, group avatar/icon, settings gear icon |
| **Tabs** | "Expenses" (default), "Balances", "Members" |
| **FAB** | "Add Expense" (always visible) |

**Expenses Tab:**

| Element | Specification |
|---|---|
| **Expense list** | Chronological list (newest first). Each item: title, amount, payer name, date, split summary icon/text, category icon (if categories are used) |
| **Deleted entries** | Shown with strikethrough styling, muted opacity, and a "deleted" badge/label. Still visible in the list for audit trail. |
| **Edited entries** | Show a small "edited" indicator (e.g., pencil icon or "edited" text near timestamp) |
| **Filters** | By date range, by member, by category (if categories exist) |
| **Search** | Search expenses by title or description |
| **Empty state** | "No expenses yet — add one!" + CTA |
| **Tap action** | Opens Expense Detail screen |

**Balances Tab:**

| Element | Specification |
|---|---|
| **Balance list** | Simplified debts within the group. Each row: "Alice owes Bob €15.00". Show directional arrows or icons. |
| **User's balance** | Highlighted at top: "You owe €X" or "You are owed €X" |
| **Settle up CTA** | Button per debt row: "Settle" → opens settlement confirmation |
| **Multi-currency note** | If expenses exist in multiple currencies, show individual currency balances AND a converted total in the group's default currency |
| **Empty state** | "All settled up! 🎉" |

**Members Tab:**

| Element | Specification |
|---|---|
| **Member list** | Name, avatar/initials, role (if any), individual balance |
| **Placeholder members** | Visually distinct (e.g., dashed border avatar, "Pending" badge) to indicate they haven't linked a real account yet |
| **Invite action** | "Invite Members" button → share invite link or enter email |
| **Manage** | Remove member option (with confirmation) |

#### 6.4.4 Join Group (via Invite Link)

**Purpose:** Flow for a user who opens a group invite link.

| Step | Specification |
|---|---|
| **Step 1** | Landing screen: group name, group description, member count, inviter name |
| **Step 2 — Account check** | If not logged in: prompt to Sign Up, Sign In, or Continue as Guest |
| **Step 3 — Identity selection** | "Join as new member" (default) **OR** "I'm already in this group" → shows list of placeholder members to claim. User picks their placeholder name → confirmation dialog: "Join as [Name]?" |
| **Step 4** | Success: navigate to Group Detail |

**States:**
- Valid link (shows group preview)
- Expired / invalid link (error screen with "Ask for a new invite" message)
- Already a member (info: "You're already in this group" + navigate to it)
- Loading

---

### 6.5 Add / Edit Expense

**Purpose:** Log a new shared expense or edit an existing one.

**Layout:** Full-screen on mobile, panel/dialog on tablet/desktop.

| Element | Specification |
|---|---|
| **Title field** | Text input, required (e.g., "Dinner at Luigi's") |
| **Amount field** | Numeric input with currency symbol. Large, prominent — this is the primary data point. |
| **Currency selector** | Defaults to group's default currency. Tap to change — shows currency picker (searchable list). |
| **Date picker** | Defaults to today. Tap to open date picker. |
| **Payer selector** | "Paid by" — shows current user by default. Tap to select a different group member. Support for **multiple payers** (split the payment itself). |
| **Split method selector** | Segmented control or chip group: Equal · Exact · Percentage · Shares |
| **Split detail area** | Changes based on selected method (see below). |
| **Description / notes** | Optional multiline text field. |
| **Category selector** | See [Open Items](#12-open-items-for-designer-decision). |
| **Save CTA** | "Add Expense" (create) or "Save Changes" (edit) |
| **Cancel** | Back arrow or "Cancel" button — confirm discard if there are unsaved changes |

**Split Method Details:**

| Method | UI |
|---|---|
| **Equal** | Shows member list with checkboxes (all checked by default). Uncheck to exclude. Shows calculated per-person amount. |
| **Exact** | Shows member list with amount input next to each. Running total at bottom must equal the expense amount. Show remaining amount. |
| **Percentage** | Shows member list with percentage input next to each. Running total must equal 100%. Show remaining %. |
| **Shares** | Shows member list with share count (stepper: +/−) next to each. Shows calculated per-person amount based on shares. |

**States:**
- Create mode (empty form)
- Edit mode (pre-populated, title says "Edit Expense")
- Validation error (amount is 0, split doesn't add up, no payer selected)
- Saving (loading indicator on save button)
- Unsaved changes warning (on back/cancel)

---

### 6.6 Expense Detail

**Purpose:** View full details of a single expense, including split breakdown and edit history.

| Element | Specification |
|---|---|
| **Header** | Title, amount (large), currency, date |
| **Paid by** | Payer name(s) with avatar |
| **Split breakdown** | List of all members involved, with individual amounts |
| **Category** | Category icon + label (if used) |
| **Notes** | Description text (if provided) |
| **Edit history** | Collapsible section: "History" — shows all changes (created, edited, deleted) with timestamps and actor names |
| **Actions** | Edit button, Delete button (with confirmation dialog) |
| **Deleted state** | If the expense is deleted: show all info with muted/strikethrough styling + "This expense was deleted by [Name] on [Date]" banner |

---

### 6.7 Settlement / Settle Up

**Purpose:** View simplified debts and mark them as settled.

This is not a separate top-level screen — it's accessed from the **Balances tab** within a Group Detail, or from the **Home dashboard**.

#### Settle Up Flow

| Step | Specification |
|---|---|
| **Entry point** | "Settle Up" button on a specific debt row |
| **Confirmation dialog** | "Mark as settled?" — shows: "You paid [Name] €23.50" or "[Name] paid you €23.50". Optional: amount field to log partial settlement. |
| **Primary CTA** | "Mark as Settled" |
| **Secondary CTA** | "Cancel" |
| **After settlement** | Debt updates in balances, event appears in activity feed: "[You] settled €23.50 with [Name]" |

**Note:** No payment integration. "Settle" = "Record that this debt was resolved outside the app."

---

### 6.8 Activity Feed

**Purpose:** Chronological log of all events across all groups (or within a single group).

| Element | Specification |
|---|---|
| **Top app bar** | Title: "Activity" (Headline Large, bold), filter icon, **profile avatar circle** (end/right) |
| **Feed items** | Each item: icon (type-specific), description text, group name (in global feed), timestamp, actor avatar |
| **Event types** | Expense created, Expense edited, Expense deleted, Member joined, Member left, Group created, Settlement recorded, Reminder sent |
| **Grouping** | Group by date ("Today", "Yesterday", "March 22", etc.) |
| **Filtering** | By group, by event type |
| **Tap action** | Navigate to the relevant entity (expense detail, group, etc.) |
| **Empty state** | "No activity yet" |

**Event item visual structure:**
```
[Icon] [Actor avatar] [Actor name] [action verb] [object] in [Group name]
       [Detail text — e.g., "€45.00 for Dinner"]
       [Timestamp — e.g., "2 hours ago"]
```

**Deleted entries in feed:** Show with a strikethrough or muted treatment, but keep them visible. Include the deletion event as its own entry ("Alice deleted 'Dinner at Luigi's'").

---

### 6.9 Profile & Settings

**Purpose:** User account management and app preferences.

**Entry point:** Accessed via the **profile avatar button** in the top app bar, which is present on all main screens (Home, Groups, Activity). Tapping the avatar opens the quick-action menu (bottom sheet on mobile, popover on tablet/desktop). "View Profile" from that menu opens the full Profile screen as a pushed navigation destination.

#### Profile Screen

| Element | Specification |
|---|---|
| **User info section** | Avatar (or initials circle), name, email |
| **Edit profile** | Tap to edit name / avatar |
| **Account section** | Change password, Link account (guest → registered), Delete account |
| **Guest-specific** | If guest: prominent "Create Account" card (upgrade CTA to protect data) |

#### Settings Screen

| Element | Specification |
|---|---|
| **Default currency** | Picker — auto-detected from locale, user can override |
| **Theme** | Light / Dark / System (segmented control or radio list) |
| **Notifications** | Toggle: push notifications on/off. Sub-toggles: expense reminders, settlement reminders, group activity |
| **Language** | Language picker (auto-detected, user can override) |
| **Support the Developers** | Card or list item → Donation screen |
| **Feedback** | "Rate the app" / "Send feedback" |
| **About** | Version number, open source licenses, project links |

#### Donation Screen

| Element | Specification |
|---|---|
| **Tone** | Warm, grateful, non-pressuring. "TabMates is free & open source. If you enjoy it, consider buying us a coffee ☕" |
| **Options** | Predefined amounts (e.g., €1, €3, €5, custom) or link to external donation page |
| **Skip** | Easy to dismiss — this must never feel like a paywall |

---

### 6.10 Notifications

**Purpose:** Show push notification history and in-app alerts.

**Note:** This may be combined with the Activity feed screen (as a filter/tab) rather than a fully separate screen. See [Open Items](#12-open-items-for-designer-decision).

| Element | Specification |
|---|---|
| **Notification items** | Similar to activity feed items but focused on actionable items (you were added to a group, someone added an expense, settlement reminder) |
| **Unread indicator** | Dot badge on items, count badge on bell icon |
| **Mark as read** | Swipe or tap to mark read, "Mark all as read" action |
| **Tap action** | Navigate to relevant screen |

---

## 7. Responsive Layout Rules

### 7.1 Mobile (< 600 dp)

```
┌──────────────────────────┐
│ [Logo]      [🔔] [👤]   │  ← Top App Bar (notification bell + profile avatar)
├──────────────────────────┤
│                          │
│                          │
│   Content Area           │
│   (single column)        │
│                          │
│                    [FAB] │
├──────────────────────────┤
│    Home │ Groups │ Act   │  ← Bottom Navigation Bar (3 items, floating pill style)
└──────────────────────────┘
```

- **Navigation:** Bottom navigation bar — **3 destinations: Home, Groups, Activity** (floating pill style per M3 Expressive / Android 16)
- **Profile:** Avatar in top app bar (top-right) — tapping opens a bottom sheet quick menu
- **Content:** Full-screen, single-column layout
- **Detail screens:** Push onto navigation stack (full screen)
- **Add Expense:** Full-screen
- **Dialogs:** Bottom sheets preferred over centered dialogs
- **FAB:** Floating Action Button for "Add Expense" (bottom-right, above nav bar)

### 7.2 Tablet (600–1200 dp)

```
┌───┬──────────────────────────────────┐
│   │  [Logo]            [🔔] [👤]    │  ← Top App Bar
│ N │                                  │
│ a ├─────────────┬────────────────────┤
│ v │             │                    │
│   │  List       │    Detail          │
│ R │  Panel      │    Panel           │
│ a │             │                    │
│ i │             │                    │
│ l │             │                    │
└───┴─────────────┴────────────────────┘
  ↑
  Home
  Groups
  Activity
  (3 items)
```

- **Navigation:** Navigation rail (left side) — **3 items: Home, Groups, Activity**
- **Profile:** Avatar in top app bar (top-right) — tapping opens a popover quick menu
- **Content:** Two-panel layout (list/master + detail)
  - Groups: Group list (left) + Group detail (right)
  - Home: Dashboard content (left) + Selected item detail (right)
  - Activity: Feed (left) + Event detail (right)
- **Add Expense:** Side panel or large dialog (not full-screen)
- **FAB:** In the content area, or action in the top app bar

### 7.3 Desktop (> 1200 dp)

```
┌────────┬────────────┬────────────────────┐
│[Logo]  │            │ [Logo] [🔔] [👤]  │  ← Top App Bar (detail panel)
│        │  List      │                    │
│ Home   │  Panel     │    Detail          │
│ Groups │            │    Panel           │
│ Activity│           │                    │
│        │            │                    │
│        │            │                    │
│        │            │                    │
│        │            │                    │
│ [👤]  │            │                    │  ← Avatar at bottom of sidebar
└────────┴────────────┴────────────────────┘
```

- **Navigation:** Persistent sidebar (expanded labels) — **3 destinations: Home, Groups, Activity**
- **Profile:** Avatar at **bottom of sidebar** (following Google Drive / Google Maps pattern) AND optionally in top bar — tapping opens a dropdown quick menu
- **Content:** Three-panel layout (nav + list + detail)
- **Add Expense:** Modal dialog or inline panel
- **Hover states:** All interactive elements must have hover states
- **Keyboard navigation:** Tab order, Enter to activate, Escape to close dialogs
- **Panel widths:** Nav sidebar ~240 dp, List panel ~320 dp, Detail panel flexible

---

## 8. Component States

All interactive components must be designed in the following states:

### 8.1 Buttons (Filled, Outlined, Text, FAB)

| State | Visual Treatment |
|---|---|
| **Default** | Standard appearance |
| **Hover** | Slight elevation increase + state layer (8% opacity) |
| **Pressed** | State layer (12% opacity) + ripple |
| **Focused** | Focus ring (2dp outline, primary color) |
| **Disabled** | 38% opacity, no interaction |
| **Loading** | Replace label with circular progress indicator, disabled interaction |

### 8.2 Text Fields

| State | Visual Treatment |
|---|---|
| **Default** | Outlined or filled variant, placeholder text |
| **Focused** | Border color changes to primary, label floats |
| **Filled** | Label remains floated, text visible |
| **Error** | Border color changes to error, supporting text shows error message |
| **Disabled** | Muted colors, no interaction |

### 8.3 Cards (Group Card, Expense Card, Balance Card)

| State | Visual Treatment |
|---|---|
| **Default** | Elevation level 1 |
| **Hover** | Elevation level 2 + subtle scale or highlight |
| **Pressed** | Elevation level 0 + state layer |
| **Selected** | Outlined with primary color (in list-detail views on tablet/desktop) |

### 8.4 List Items

| State | Visual Treatment |
|---|---|
| **Default** | Standard appearance |
| **Hover** | Background state layer |
| **Pressed** | Ripple |
| **Selected** | Primary container color background |
| **Swipe actions** | Swipe left: delete (red background). Swipe right: edit/settle (contextual). |

### 8.5 Screen-Level States

Every screen that loads data must support:

| State | Treatment |
|---|---|
| **Loading** | Skeleton placeholders (shimmer) that match the shape of real content |
| **Empty** | Illustration + message + CTA (context-specific) |
| **Error** | Error illustration + message + "Retry" button |
| **Populated** | Normal content |
| **Pull-to-refresh** | Material 3 pull refresh indicator (mobile) |

---

## 9. Accessibility Requirements

### 9.1 Color & Contrast

| Requirement | Specification |
|---|---|
| **Normal text** | ≥ 4.5:1 contrast ratio (WCAG AA) |
| **Large text** (≥ 18sp or ≥ 14sp bold) | ≥ 3:1 contrast ratio |
| **Interactive elements** | ≥ 3:1 against adjacent colors |
| **Don't rely on color alone** | Always pair color with icons, text labels, or patterns (e.g., "you owe" vs "you are owed" should have both color AND text) |

### 9.2 Touch Targets

| Requirement | Specification |
|---|---|
| **Minimum touch target** | 48 × 48 dp |
| **Recommended** | 48 × 48 dp with 8 dp spacing between targets |
| **Icon buttons** | Even if icon is 24 dp, touch area is 48 dp |

### 9.3 Screen Reader & Focus

| Requirement | Specification |
|---|---|
| **Content descriptions** | All images, icons, and decorative elements must have content descriptions or be marked as decorative |
| **Focus order** | Logical reading order, top-to-bottom, left-to-right |
| **Headings** | Proper heading hierarchy for screen readers |
| **Live regions** | Balance updates, error messages, and snackbars should be announced |
| **Custom actions** | Swipe actions on list items must have accessible alternatives (e.g., long-press menu) |

---

## 10. Motion & Animation

### Principles

Following M3 Expressive motion — the same motion language used in Google Photos, Google Contacts, and Android 16 system apps:

| Principle | Description |
|---|---|
| **Meaningful** | Motion communicates relationships (e.g., expanding a card shows it contains detail) |
| **Focused** | Draw attention to important changes (balance update, new expense) |
| **Expressive** | Spring-based curves with slight overshoot for a playful, physical feel — not linear or ease-in-out |
| **Springy** | Use spring physics (low stiffness, moderate damping) for sheet reveals, FAB morphs, and card expansions. Matches Google Photos and Android 16 style. |
| **Continuous** | Avoid abrupt cuts. Chain animations so transitions feel fluid end-to-end (e.g., FAB morphs into a bottom sheet rather than disappearing and reappearing). |

### Spring Physics Reference

M3 Expressive standardizes on spring-based motion. Use these values as the baseline — the designer should annotate deviations:

| Parameter | Value | Notes |
|---|---|---|
| **Stiffness** | 380 | Standard spring stiffness for most transitions |
| **Damping ratio** | 0.8 | Slightly underdamped — allows a very brief overshoot |
| **Duration cap** | 500 ms | Springs are duration-less by physics; cap at 500 ms for usability |
| **Enter duration** | ~350 ms | Screens/sheets entering (spring feel) |
| **Exit duration** | ~200 ms | Screens/sheets exiting (faster, less spring) |

### Key Animations

| Animation | Specification |
|---|---|
| **Screen transitions** | Shared element transitions where possible (e.g., group card → group detail). Use predictive back gesture (Android 14+). |
| **Navigation tab switch** | Spring scale + crossfade on the active indicator pill — matches Android 16 nav bar behavior |
| **List item entry** | Staggered fade-in + slight upward slide when loading (spring, stagger 30 ms per item) |
| **FAB** | Scale animation on appear; **morphs** into bottom sheet on tap (continuous spring transition) — not a separate open animation |
| **Profile avatar tap** | Subtle scale pop (1.0 → 1.1 → 1.0 spring) then bottom sheet / popover slides in |
| **Balance changes** | Animated counter — number rolls to new value using a spring ticker |
| **Pull-to-refresh** | M3 standard refresh indicator |
| **Swipe to delete** | Slide + fade with undo snackbar (snackbar itself springs in from the bottom) |
| **Bottom sheet** | Spring-based slide up with scrim fade; slight overshoot allowed on open |
| **Skeleton loading** | Shimmer effect (left-to-right gradient sweep, 1200 ms loop) |
| **Shared element hero** | Group card avatar / name morphs into Group Detail header (shared element transition, spring-driven) |

---

## 11. Localization Considerations

| Requirement | Specification |
|---|---|
| **Languages** | Multi-language from launch (English as primary, additional TBD) |
| **RTL** | Not at launch, but **all layouts must be RTL-ready**: use `start`/`end` instead of `left`/`right`, avoid hardcoded text positioning |
| **Text expansion** | Design with ~40% text expansion buffer (German, for example, is significantly longer than English). Buttons and labels must not truncate. |
| **Currency formatting** | Auto-detect from locale. Support: `€1.234,56` (DE), `$1,234.56` (US), `¥1,234` (JP), etc. |
| **Number formatting** | Locale-aware decimal separators and grouping |
| **Date formatting** | Locale-aware (DD/MM/YYYY vs MM/DD/YYYY). Use relative dates where possible ("2 hours ago", "Yesterday"). |
| **Pluralization** | "1 expense" vs "2 expenses" — design must accommodate plural rules (some languages have >2 plural forms) |

---

## 12. Open Items for Designer Decision

The following items need designer input or a joint decision before finalizing mockups. These are not blockers — the designer can propose solutions as part of the mockup iteration.

### 12.1 Expense Categories

**Question:** Should expenses have predefined categories with icons?

**Recommendation:** Yes — a predefined set with icons makes the UI more scannable and visually interesting. Suggested starter set:

| Category | Icon | Emoji fallback |
|---|---|---|
| Food & Drink | 🍽️ restaurant icon | 🍽️ |
| Groceries | 🛒 shopping cart | 🛒 |
| Transport | 🚗 car icon | 🚗 |
| Accommodation | 🏨 hotel icon | 🏨 |
| Entertainment | 🎉 celebration icon | 🎉 |
| Shopping | 🛍️ shopping bag | 🛍️ |
| Utilities | ⚡ bolt icon | ⚡ |
| Health | 💊 medical icon | 💊 |
| Other | 📦 package icon | 📦 |

**Decision needed:** Should users also be able to create custom categories? (Adds complexity.)

### 12.2 User & Group Avatars

**Question:** How should user and group identity be visually represented?

**Recommendation:**
- **Users:** Auto-generated initials circle (colored based on name hash) as default, with optional photo upload.
- **Groups:** Auto-generated icon/emoji based on group name, or selectable from a predefined set of icons. Optional photo upload.

**Decision needed:** Is photo upload in scope for MVP, or just auto-generated visuals?

### 12.3 Notifications vs. Activity

**Question:** Are "Notifications" and "Activity" two separate screens, or unified?

**Recommendation:** Unify them into a single "Activity" tab in the bottom nav. Show a notification badge on the tab for unread items. The Activity screen shows all events chronologically with read/unread state. Push notifications link directly to the relevant item in this feed.

This avoids a 5th bottom nav item and reduces confusion.

### 12.4 Donation Screen Prominence

**Question:** How visible should the donation/support screen be?

**Recommendation:** A list item in Settings/Profile ("Support the Developers ❤️"). Not shown in onboarding or as a popup. Optionally shown as a subtle card on the Home screen after the user has been active for 2+ weeks. Never intrusive.

---

## Appendix A: Deliverable Checklist for Designers

| # | Deliverable | Format |
|---|---|---|
| 1 | Brand identity: logo, wordmark, app icon, favicon | Vector (SVG) + raster exports |
| 2 | Complete M3 color scheme (light + dark) with hex values | Design tokens spreadsheet or Figma variables |
| 3 | Typography scale applied to platform fonts | Figma text styles |
| 4 | Icon set selection (Material Symbols Rounded) | Confirmed style + any custom icons |
| 5 | Illustration style guide + empty state illustrations | SVG / vector |
| 6 | Mobile mockups — all screens listed in Section 6 (light + dark) | Figma frames (360–412 dp width) |
| 7 | Tablet mockups — key screens with two-panel layout | Figma frames (800–1000 dp width) |
| 8 | Desktop mockups — key screens with three-panel layout | Figma frames (1280–1440 dp width) |
| 9 | Component states documentation (Section 8) | Annotated Figma components |
| 10 | Prototype — core flow: Create Group → Add Expense → View Balances → Settle | Interactive Figma prototype |
| 11 | Accessibility annotations | Annotated on mockups |
| 12 | Motion specs (timing, easing curves) | Annotation or video references |

---

## Appendix B: Technical Constraints for Designers

These are implementation details that affect design decisions:

| Constraint                      | Impact                                                                                                                                                                                              |
|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Compose Multiplatform**       | All UI is built with Compose. Custom native widgets are not available (except iOS navigation). Design within M3 component capabilities.                                                             |
| **M3 Expressive component set** | Use Material 3 Expressive components: NavigationBar (3-item floating pill variant), NavigationRail, NavigationDrawer, TopAppBar, LargeTopAppBar (collapsing), ExtendedFAB, BottomSheet, Chips, Cards. Custom components are possible but expensive. |
| **Spring motion (Compose)**     | Compose `spring()` animationSpec is used for all transitions. Shared element transitions are available via `SharedTransitionLayout` in Compose. Design motion with these constraints in mind.        |
| **Dynamic color (Android 12+)** | The designed color scheme is a fallback. On Android 12+, the system may override colors based on the user's wallpaper. Design should look good even with shifted hues. Fixed color tokens (`primaryFixed` etc.) are NOT affected by dynamic color. |
| **iOS: SF Pro + native nav**    | On iOS, typography uses SF Pro and navigation uses native transitions. The rest of the design (colors, shapes, illustrations) is shared.                                                            |
| **No image CDN yet**            | User avatars (if photo upload is supported) will be stored server-side. Keep image dimensions reasonable.                                                                                           |
| **Offline-first for guests**    | Guest mode works fully offline. Design must not assume network availability for core flows.                                                                                                         |
| **Material 3 Icons**            | Do not use any emojis but Material 3 Icons instead.                                                                                                                                                 |

---

*End of document. Ready for designer handoff.*

