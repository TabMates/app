# TabMates — Mockup Requirements Document

> **Purpose:** Self-contained design brief for producing high-fidelity mockups.
> Designers should be able to work from this document alone.
>
> **Last updated:** 2026-03-25
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

### Personality & Tone

| Attribute | Direction |
|---|---|
| **Overall feel** | Warm & social — friend-group vibes |
| **Visual style** | Colorful, playful, approachable |
| **Design language** | Material 3 Expressive (rounded shapes, vibrant palette, expressive motion) |
| **NOT** | Corporate, sterile, cold, overly minimal |

The app should feel like a friendly tool that makes an awkward topic (money between friends) feel casual and stress-free.

### Color Direction

**The full palette is open for redesign.** The designer has creative freedom to propose a new palette that matches the warm & social personality. Consider:

- A primary color that feels friendly and energetic
- Rich secondary/tertiary colors for visual interest
- Both light and dark mode variants
- Android 12+ dynamic color support (the theme must work as a fallback when dynamic color is unavailable)

---

## 4. Design System Specification

### 4.1 Color Palette

Deliver a complete Material 3 color scheme:

| Token | Light Mode | Dark Mode |
|---|---|---|
| `primary` | TBD | TBD |
| `onPrimary` | TBD | TBD |
| `primaryContainer` | TBD | TBD |
| `onPrimaryContainer` | TBD | TBD |
| `secondary` | TBD | TBD |
| `onSecondary` | TBD | TBD |
| `secondaryContainer` | TBD | TBD |
| `onSecondaryContainer` | TBD | TBD |
| `tertiary` | TBD | TBD |
| `onTertiary` | TBD | TBD |
| `tertiaryContainer` | TBD | TBD |
| `onTertiaryContainer` | TBD | TBD |
| `error` | TBD | TBD |
| `errorContainer` | TBD | TBD |
| `onError` | TBD | TBD |
| `onErrorContainer` | TBD | TBD |
| `background` | TBD | TBD |
| `onBackground` | TBD | TBD |
| `surface` | TBD | TBD |
| `onSurface` | TBD | TBD |
| `surfaceVariant` | TBD | TBD |
| `onSurfaceVariant` | TBD | TBD |
| `outline` | TBD | TBD |
| `outlineVariant` | TBD | TBD |
| `inverseSurface` | TBD | TBD |
| `inverseOnSurface` | TBD | TBD |
| `inversePrimary` | TBD | TBD |
| `surfaceTint` | TBD | TBD |
| `scrim` | TBD | TBD |

Additionally, define **semantic colors** for expense-specific contexts:

| Semantic Token | Usage |
|---|---|
| `positive` / `youAreOwed` | When the user is owed money (green direction) |
| `negative` / `youOwe` | When the user owes money (red direction) |
| `settled` | When a debt is fully settled (neutral/muted) |
| `deleted` | Visual treatment for deleted entries in the activity feed |

### 4.2 Typography Scale

Follow the **M3 type scale**. The app uses the platform default font on each platform:

| Platform | Font |
|---|---|
| Android | Roboto / system default |
| iOS | SF Pro |
| Web / Desktop | System sans-serif stack |

Define sizes for:

| Style | Size | Weight | Line Height | Usage |
|---|---|---|---|---|
| Display Large | 57sp | 400 | 64sp | — |
| Display Medium | 45sp | 400 | 52sp | — |
| Display Small | 36sp | 400 | 44sp | — |
| Headline Large | 32sp | 400 | 40sp | Screen titles |
| Headline Medium | 28sp | 400 | 36sp | Section headers |
| Headline Small | 24sp | 400 | 32sp | Card titles |
| Title Large | 22sp | 400 | 28sp | Top app bar |
| Title Medium | 16sp | 500 | 24sp | List item titles |
| Title Small | 14sp | 500 | 20sp | — |
| Body Large | 16sp | 400 | 24sp | Primary body text |
| Body Medium | 14sp | 400 | 20sp | Secondary text |
| Body Small | 12sp | 400 | 16sp | Captions, timestamps |
| Label Large | 14sp | 500 | 20sp | Buttons, tabs |
| Label Medium | 12sp | 500 | 16sp | Chips, badges |
| Label Small | 11sp | 500 | 16sp | Overlines |

**Note:** For the "M3 Expressive" direction, feel free to explore bolder headline weights or slightly oversized display styles for key moments (e.g., balance amounts, settlement summaries).

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

M3 Expressive favors **generous corner radii**:

| Token | Radius | Usage |
|---|---|---|
| Extra Small | 4 dp | Badges, small chips |
| Small | 8 dp | Chips, text fields |
| Medium | 12 dp | Cards, list items |
| Large | 16 dp | FAB, dialogs |
| Extra Large | 28 dp | Bottom sheets, large cards |
| Full | 50% | Avatar circles, toggle tracks |

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
│   └── Guest Mode (skip auth, device only data, no way of signing in on the same account on another device, and data is lost after clearing app data)
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
├── ★ Profile
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

> **★** = Top-level bottom navigation destination (mobile) / sidebar item (tablet/desktop)

### 5.2 Navigation Patterns by Breakpoint

| Breakpoint | Navigation | Detail Behavior |
|---|---|---|
| **Mobile** (< 600 dp) | Bottom navigation bar (4 items: Home, Groups, Activity, Profile) | Full-screen push navigation |
| **Tablet** (600–1200 dp) | Navigation rail (left side) | Two-panel: list + detail side by side |
| **Desktop** (> 1200 dp) | Persistent sidebar navigation | Three-panel: sidebar + list + detail |

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
| **Top app bar** | "TabMates" wordmark or logo, notification bell icon |
| **Balance summary card** | Large, prominent card showing: net balance ("You are owed €45.20" or "You owe €12.00"), color-coded (positive = green, negative = red, settled = neutral) |
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
| **Top app bar** | Title: "Groups", search icon |
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
┌─────────────────────┐
│    Top App Bar      │
├─────────────────────┤
│                     │
│                     │
│   Content Area      │
│   (single column)   │
│                     │
│                     │
│              [FAB]  │
├─────────────────────┤
│ Home│Groups│Act│Prof│  ← Bottom Navigation Bar
└─────────────────────┘
```

- **Navigation:** Bottom navigation bar (4 destinations)
- **Content:** Full-screen, single-column layout
- **Detail screens:** Push onto navigation stack (full screen)
- **Add Expense:** Full-screen
- **Dialogs:** Bottom sheets preferred over centered dialogs
- **FAB:** Floating Action Button for "Add Expense" (bottom-right, above nav bar)

### 7.2 Tablet (600–1200 dp)

```
┌───┬──────────────────────────────┐
│   │       Top App Bar            │
│ N │                              │
│ a ├────────────┬─────────────────┤
│ v │            │                 │
│   │  List      │    Detail       │
│ R │  Panel     │    Panel        │
│ a │            │                 │
│ i │            │                 │
│ l │            │                 │
└───┴────────────┴─────────────────┘
```

- **Navigation:** Navigation rail (left side, icons + labels)
- **Content:** Two-panel layout (list/master + detail)
  - Groups: Group list (left) + Group detail (right)
  - Home: Dashboard content (left) + Selected item detail (right)
  - Activity: Feed (left) + Event detail (right)
- **Add Expense:** Side panel or large dialog (not full-screen)
- **FAB:** In the content area, or action in the top app bar

### 7.3 Desktop (> 1200 dp)

```
┌────────┬────────────┬────────────────────┐
│        │            │                    │
│ Side   │  List      │    Detail          │
│ bar    │  Panel     │    Panel           │
│ Nav    │            │                    │
│        │            │                    │
│ Home   │            │                    │
│ Groups │            │                    │
│ Activity│           │                    │
│ Profile│            │                    │
│        │            │                    │
│        │            │                    │
└────────┴────────────┴────────────────────┘
```

- **Navigation:** Persistent sidebar (expanded labels)
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

Following M3 Expressive motion:

| Principle | Description |
|---|---|
| **Meaningful** | Motion communicates relationships (e.g., expanding a card shows it contains detail) |
| **Focused** | Draw attention to important changes (balance update, new expense) |
| **Expressive** | Spring-based curves, slight overshoot for playful feel |

### Key Animations

| Animation | Specification |
|---|---|
| **Screen transitions** | Shared element transitions where possible (e.g., group card → group detail) |
| **List item entry** | Staggered fade-in when loading |
| **FAB** | Scale animation on appear, morph to expanded state if used |
| **Balance changes** | Animated counter (number rolls to new value) |
| **Pull-to-refresh** | M3 standard refresh indicator |
| **Swipe to delete** | Slide + fade with undo snackbar |
| **Bottom sheet** | Spring-based slide up with scrim |
| **Skeleton loading** | Shimmer effect (left-to-right gradient sweep) |

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
| **M3 component set**            | Stick to the Material 3 component library (buttons, cards, chips, lists, dialogs, bottom sheets, FAB, navigation bar/rail/drawer, top app bar, etc.). Custom components are possible but expensive. |
| **Dynamic color (Android 12+)** | The designed color scheme is a fallback. On Android 12+, the system may override colors based on the user's wallpaper. Design should look good even with shifted hues.                              |
| **iOS: SF Pro + native nav**    | On iOS, typography uses SF Pro and navigation uses native transitions. The rest of the design (colors, shapes, illustrations) is shared.                                                            |
| **No image CDN yet**            | User avatars (if photo upload is supported) will be stored server-side. Keep image dimensions reasonable.                                                                                           |
| **Offline-first for guests**    | Guest mode works fully offline. Design must not assume network availability for core flows.                                                                                                         |
| **Material 3 Icons**            | Do not use any emojis but Material 3 Icons instead.                                                                                                                                                 |

---

*End of document. Ready for designer handoff.*

