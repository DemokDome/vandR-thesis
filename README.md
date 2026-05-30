<div align="center">

# vandR

**A cross-platform group travel companion built with Kotlin Multiplatform and Compose Multiplatform.**

Plan trips together — organise places to visit, split expenses fairly, chat with travelling companions, and let an AI assistant help build the itinerary. One Kotlin codebase ships to native Android and the browser (JS) from the same shared module.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.3-4285F4?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20Web-blue)]()

</div>

---

## Overview

vandR is an end-to-end demonstration of what a modern KMP + Compose Multiplatform application looks like in practice. It was developed as the topic of a BSc thesis at BME (Budapest University of Technology and Economics) and exercises the platform across the layers a serious app needs: shared UI and business logic, real-time multi-user sync, third-party API integration, on-device caching, push notifications, and full localisation — all from a single Kotlin codebase that targets Android and the browser.

The user-facing experience is designed for a concrete scenario: a small group of people planning a trip together. They invite each other via a 6-character join code or QR link, build up a list of places to visit using Foursquare, add and split shared expenses, chat in real time, ask a Gemini-powered assistant for suggestions, and assemble the day-by-day itinerary together.

## Highlights

- **One codebase, two targets.** The Android app and the web app share their UI, view models, repositories, navigation, and i18n layer. Platform-specific code (Firebase clients, SQLite driver, logging, geocoding) is provided through the standard `expect` / `actual` pattern.
- **Real-time collaboration.** Trip data, places, expenses, and chat messages are streamed via Firestore snapshot listeners. Multi-device updates appear without manual refresh.
- **AI travel assistant.** Each trip has its own Gemini chat thread that receives the trip's destination, dates, member count, and the saved-places context as a system prompt — answers stay grounded in the actual itinerary.
- **Smart expense settlement.** Add expenses, choose how to split them (equally or custom amounts), and the budget tab computes the **minimum-transaction debt-settlement** between members, not just naive pair-wise debts.
- **Offline-first on Android.** Trips, places, and other entities are cached in a local SQLDelight database. The web target is online-only and skips the local cache by design.
- **Bilingual (English / Hungarian).** Every user-facing string flows through a `Strings` data class behind a `CompositionLocal`. Number, date, and pluralisation rules are locale-aware.
- **Proper Material 3 polish.** Edge-to-edge rendering, IME-aware bottom sheets, dark mode, and adaptive layouts that switch from a bottom navigation bar on Android to a side rail on the web.

## Feature tour

| Area | What it does |
| --- | --- |
| **Authentication** | Email + password sign-up / sign-in with Firebase Auth, password reset by email, account deletion with re-authentication. |
| **Trips** | Create, edit, delete, favourite. Shareable 6-character code and `travelplanner://join/{code}` deep link with QR scanning on Android. |
| **Places** | Foursquare-powered search by destination + category filters, save with notes, distance / rating / photo enrichment. |
| **Map** | Google Maps view of saved places, day-by-day filtering tied into the itinerary. |
| **Itinerary** | Day-grid planner; assign saved places to specific days, drag-style reordering. |
| **Budget** | Categorised expense entries, per-person summary, optimal debt graph (minimum number of transfers to settle), historical names preserved when members leave. |
| **Group chat** | Per-trip real-time message thread with sender attribution. |
| **AI assistant** | Per-trip Gemini chat with trip + places context injected automatically. |
| **Gallery** | Trip photos uploaded to Firebase Storage, shared between all members. |
| **Inbox** | Aggregated activity feed (new chat messages, expense / place adds and removals, member joins / leaves) with dismiss & clear-all. |
| **Profile** | Display name, theme (system / light / dark), language switch, sign out, delete account. |
| **Push notifications** | Firebase Cloud Messaging with a notification channel for trip activity (Android). |
| **Offline mode** | Read-only banner + degraded actions while offline; reads served from the SQLDelight cache. |

## Architecture

```
┌───────────────────────────────────────────────────────────────────┐
│                      Compose Multiplatform UI                     │
│  Screens · Navigation 3 · Material 3 theming · i18n Composition   │
└────────────────┬─────────────────────────────────┬────────────────┘
                 │                                 │
        ┌────────▼─────────┐               ┌───────▼────────┐
        │   ViewModels     │               │   Strings /    │
        │ StateFlow + Koin │               │  LocalStrings  │
        └────────┬─────────┘               └────────────────┘
                 │
       ┌─────────▼──────────┐         ┌──────────────────┐
       │   Repositories     │◀────────│   expect/actual  │
       │  (interface in     │         │  platform impls  │
       │   commonMain)      │         │ (Android · JS)   │
       └─┬─────────┬─────┬──┘         └──────────────────┘
         │         │     │
   ┌─────▼──┐ ┌────▼──┐ ┌▼──────────────┐
   │Firebase│ │Foursq.│ │  Gemini API   │
   │ + FCM  │ │ Places│ │  (Ktor JSON)  │
   └────────┘ └───────┘ └───────────────┘

   Android only ──▶ SQLDelight (offline cache)
                     android.util.Log
                     Google Maps SDK
                     FCM messaging service

   Web only ──────▶ Browser geolocation
                     console.{error,warn} logging
```

- **Pattern.** MVVM with single-source-of-truth `StateFlow` UI state, repository abstractions in `commonMain`, platform implementations in `androidMain` / `jsMain`.
- **DI.** Koin with `commonModule` (services, view models) + `expect val platformModule` (per-platform Firebase / DB / network bindings).
- **Networking.** Ktor 3 with kotlinx-serialisation; OkHttp engine on Android, JS engine on the browser.
- **Persistence.** Firestore as system-of-record. SQLDelight on Android for offline reads. Multiplatform-Settings for user prefs (theme, language).
- **Auth.** Firebase Auth on Android via the official SDK; on the web through the GitLive Firebase Kotlin wrapper.
- **Concurrency.** kotlinx.coroutines + Flow throughout; `callbackFlow` adapters wrap Firestore listeners.

## Tech stack

| Layer | Library / SDK |
| --- | --- |
| Language | Kotlin 2.3.20 (multiplatform) |
| UI | Compose Multiplatform 1.10.3, Material 3, Material 3 Adaptive |
| Navigation | androidx.navigation3 (alpha) |
| DI | Koin 4.2 |
| HTTP | Ktor 3.4 |
| Serialisation | kotlinx-serialization-json |
| Concurrency | kotlinx-coroutines |
| Date / time | kotlinx-datetime |
| Local DB (Android) | SQLDelight 2.3 |
| Image loading | Coil 3 (Compose + Ktor) |
| Settings | multiplatform-settings 1.3 |
| Auth & DB | Firebase Auth, Firestore, FCM, Storage (Android SDK + GitLive on web) |
| Places | Foursquare Places API v3 |
| AI | Google Gemini API (REST) |
| Maps | Google Maps SDK for Android |

## Project structure

```
TravelPlanner/
├── composeApp/
│   └── src/
│       ├── commonMain/kotlin/com/domedemok/travelplanner/
│       │   ├── data/
│       │   │   ├── model/        Trip, Place, Expense, ChatMessage, …
│       │   │   ├── remote/       FoursquareService, AiService, ContextBuilder
│       │   │   └── repository/   Auth · Trip · Place · Chat · AiChat · Expense · Itinerary · Photo
│       │   ├── di/               Koin modules
│       │   ├── i18n/             Strings, EnStrings, HuStrings, LanguageManager
│       │   ├── ui/
│       │   │   ├── components/   BottomNav, NewTripFlow, AddExpenseDialog, …
│       │   │   ├── navigation/   Route sealed hierarchy + nav3 wiring
│       │   │   ├── screens/      One file per top-level screen (≈ 20 of them)
│       │   │   └── theme/        Material 3 theme, gradients, spacing tokens
│       │   ├── util/             Logger (expect), NetworkMonitor, SecurityValidator
│       │   ├── viewmodel/        One ViewModel per feature area
│       │   └── App.kt            Entry composable + auth-state-aware nav shell
│       │
│       ├── androidMain/kotlin/   Firebase Android SDK, SQLDelight Android driver,
│       │                         Google Maps composable, FCM service, Logger.android
│       ├── androidMain/res/      String resources for the FCM notification channel
│       │                         (system Settings respects device locale)
│       │
│       └── jsMain/kotlin/        GitLive Firebase wrappers, browser geolocation,
│                                 Logger.js (console.{error,warn})
│
├── gradle/libs.versions.toml     Single source of truth for versions
└── README.md
```

## Build & run

The project ships with the standard Kotlin Multiplatform / Compose Multiplatform Gradle wrapper.

### Android

```bash
# Linux / macOS
./gradlew :composeApp:assembleDebug

# Windows
.\gradlew.bat :composeApp:assembleDebug
```

The minimum SDK is 24, target / compile SDK is 36.

You can also use any IDE with the Kotlin Multiplatform plugin (Android Studio, IntelliJ IDEA Ultimate) and pick the `composeApp` Android run configuration.

### Web

```bash

# JS — broader compatibility
./gradlew :composeApp:jsBrowserDevelopmentRun
```

(Replace `./gradlew` with `.\gradlew.bat` on Windows.)

A development server starts on `http://localhost:8080` by default and hot-reloads on file changes.

## Notable engineering choices

- **Single navigation backstack via Navigation 3.** All routes are typed sealed-class `Route` instances; the trip-detail tab state lives inside a single back-stack entry that hosts an inner `MainShell` so the bottom navigation never re-mounts on tab switches.
- **Locale strategy.** Compose-side strings flow through `LocalStrings.current` for instant in-app language switches; the Android notification channel uses native `R.string` resources because it is rendered by the OS Settings UI under the device locale, not the app's.
- **i18n parameter injection.** Pluralised and parameterised strings are modelled as lambdas (`(Int) -> String`, `(String, String) -> String`) on the `Strings` data class, so call sites stay idiomatic Kotlin and translation units stay self-contained.
- **Sealed result types at boundaries.** Repository operations that have multiple distinct failure modes (`joinTripByCode` → `JoinTripResult.{Success, InvalidCode, AlreadyMember, Failure}`) return typed results rather than `Result<Trip>` with stringly-typed exception messages.
- **Historical name preservation.** When a user leaves or is removed from a trip, their UID + display name are atomically moved from `tripMembers` to `formerMembers` in a single Firestore update. Security rules check active membership against `tripMembers.keys()`; UI name lookups fall back to `formerMembers` so old expense splits still render the correct name.
- **Generic bottom navigation.** `BottomNav<T : NavTabSpec>` deduplicates the main-shell and trip-detail tab bars with one composable parameterised by a tab-spec interface.
- **IME-aware sheets.** Every `ModalBottomSheet` containing a text field applies `Modifier.imePadding()` because Material 3 renders sheets in their own dialog window, where the activity-level `adjustResize` does not propagate.

## Status

This codebase is the deliverable of a BSc thesis. It is **not** maintained as a production product, but it is intended to be readable, runnable, and a faithful reference for what a real KMP + Compose Multiplatform app looks like across all the layers — UI, state, networking, persistence, security, and platform-specific glue.

## Author

**Demők Döme** — Software Engineering BSc thesis, BME, Budapest, Hungary, 2026.

## License

Academic use. The third-party services this project integrates with (Firebase, Foursquare, Google Gemini, Google Maps) are governed by their own terms of service.
