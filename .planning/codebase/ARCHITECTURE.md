# Architecture

**Analysis Date:** 2026-04-14

## Pattern Overview

**Overall:** React Native single-root component tree (scaffold state)

**Key Characteristics:**
- Single `App.tsx` root component registered via `index.js`
- Platform-specific native shells (Android Kotlin, iOS Swift) bootstrap the JS bundle
- No application logic yet — this is a fresh CLI scaffold awaiting feature development
- New Architecture enabled (Fabric renderer on Android via `fabricEnabled`, standard RCT on iOS)

## Layers

**Native Bootstrap Layer:**
- Purpose: Launch the React Native runtime and load the JS bundle
- Location: `android/app/src/main/java/com/kindnessdice/` and `ios/KindnessDice/`
- Contains: `MainActivity.kt`, `MainApplication.kt` (Android); `AppDelegate.swift` (iOS)
- Depends on: React Native host APIs (`ReactActivity`, `RCTReactNativeFactory`)
- Used by: The OS on app launch

**JS Entry Point:**
- Purpose: Register the root component with the React Native runtime
- Location: `index.js`
- Contains: `AppRegistry.registerComponent('KindnessDice', () => App)`
- Depends on: `App.tsx`, `app.json` (for the app name)
- Used by: Metro bundler; the native layer calls into this registered component name

**Root Component Layer:**
- Purpose: Render the top-level UI shell
- Location: `App.tsx`
- Contains: `App` (safe-area provider wrapper) and `AppContent` (inner view)
- Depends on: `react-native`, `react-native-safe-area-context`, `@react-native/new-app-screen`
- Used by: `index.js` via `AppRegistry`

## Data Flow

**App Launch Flow:**

1. OS starts the native activity/app delegate (`MainActivity.kt` / `AppDelegate.swift`)
2. Native layer initialises the React Native host and loads the JS bundle via Metro (debug) or bundled `.jsbundle` (release)
3. `index.js` runs; `AppRegistry.registerComponent` maps the name `"KindnessDice"` to the `App` component
4. React Native renders `App` → `SafeAreaProvider` → `AppContent` → `View` → `NewAppScreen`

**State Management:**
- No state management library present. The only runtime state is the system color scheme (`useColorScheme`) used to toggle dark/light `StatusBar` style.

## Key Abstractions

**App (root component):**
- Purpose: Provides safe-area context and status bar configuration for the entire app
- File: `App.tsx`
- Pattern: Functional component with a single layout child (`AppContent`)

**AppContent:**
- Purpose: Holds the main screen content inside safe-area insets
- File: `App.tsx` (co-located with `App`)
- Pattern: Separate functional component to allow `useSafeAreaInsets` hook usage below the provider

## Entry Points

**JavaScript Entry Point:**
- Location: `index.js`
- Triggers: Metro bundler on `npm start`; native runtime on app launch
- Responsibilities: Registers `App` component under the name `"KindnessDice"`

**Android Native Entry Point:**
- Location: `android/app/src/main/java/com/kindnessdice/MainActivity.kt`
- Triggers: Android OS activity lifecycle
- Responsibilities: Declares main component name, creates `DefaultReactActivityDelegate` with Fabric enabled

**iOS Native Entry Point:**
- Location: `ios/KindnessDice/AppDelegate.swift`
- Triggers: iOS `UIApplication` lifecycle (`didFinishLaunchingWithOptions`)
- Responsibilities: Creates `RCTReactNativeFactory`, starts React Native with module name `"KindnessDice"`, sets bundle URL

## Error Handling

**Strategy:** No custom error handling implemented yet (scaffold default).

**Patterns:**
- React Native default RedBox overlay in development for JS errors
- Native crash reporters (none configured)

## Cross-Cutting Concerns

**Logging:** No logging library configured; default `console.*` available in JS, Logcat/Xcode console on native.
**Validation:** Not applicable at scaffold stage.
**Authentication:** Not implemented.

---

*Architecture analysis: 2026-04-14*
