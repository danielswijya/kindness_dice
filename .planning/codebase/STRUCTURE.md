# Codebase Structure

**Analysis Date:** 2026-04-14

## Directory Layout

```
KindnessDice/                   # Project root
├── __tests__/                  # Jest test files (mirroring root source)
│   └── App.test.tsx            # Smoke test for the App component
├── android/                    # Android native project (Gradle/Kotlin)
│   ├── app/
│   │   └── src/main/java/com/kindnessdice/
│   │       ├── MainActivity.kt         # React Native activity
│   │       └── MainApplication.kt      # App-level React Native host setup
│   ├── build.gradle
│   └── settings.gradle
├── ios/                        # iOS native project (Xcode/Swift)
│   ├── KindnessDice/
│   │   ├── AppDelegate.swift           # iOS app launch + RN bootstrap
│   │   ├── Info.plist
│   │   ├── LaunchScreen.storyboard
│   │   └── Images.xcassets/
│   ├── KindnessDice.xcodeproj/
│   └── Podfile
├── .bundle/                    # Ruby bundler config (CocoaPods toolchain)
├── .planning/                  # GSD planning documents (not shipped)
│   └── codebase/
├── node_modules/               # JS dependencies (not committed)
├── App.tsx                     # Root React component
├── index.js                    # JS entry point — AppRegistry
├── app.json                    # App name config
├── babel.config.js             # Babel preset
├── metro.config.js             # Metro bundler config
├── jest.config.js              # Jest config
├── tsconfig.json               # TypeScript config
├── .eslintrc.js                # ESLint config
├── .prettierrc.js              # Prettier config
├── Gemfile                     # Ruby gems (CocoaPods)
├── package.json
└── package-lock.json
```

## Directory Purposes

**`__tests__/`:**
- Purpose: All Jest test files, organised to mirror the root source layout
- Contains: `*.test.tsx` / `*.test.ts` files
- Key files: `__tests__/App.test.tsx` — baseline render smoke test

**`android/`:**
- Purpose: Full Android Gradle project; not edited unless adding native modules
- Contains: Kotlin source, Gradle build files, AndroidManifest, resources
- Key files: `android/app/src/main/java/com/kindnessdice/MainActivity.kt`, `MainApplication.kt`

**`ios/`:**
- Purpose: Full Xcode project; not edited unless adding native modules
- Contains: Swift source, storyboards, asset catalogs, Podfile
- Key files: `ios/KindnessDice/AppDelegate.swift`, `ios/KindnessDice/Info.plist`

**`.bundle/`:**
- Purpose: Ruby bundler configuration for CocoaPods toolchain
- Generated: No (committed config)
- Committed: Yes

**`.planning/codebase/`:**
- Purpose: GSD codebase map documents used by planning and execution agents
- Generated: By GSD agents
- Committed: Yes

## Key File Locations

**Entry Points:**
- `index.js`: JS entry point — registers `App` with `AppRegistry`
- `android/app/src/main/java/com/kindnessdice/MainActivity.kt`: Android activity
- `ios/KindnessDice/AppDelegate.swift`: iOS application delegate

**Root UI:**
- `App.tsx`: Root React component; all UI originates here

**Configuration:**
- `app.json`: App name used by both platforms (`"KindnessDice"`)
- `tsconfig.json`: Extends `@react-native/typescript-config`; adds `jest` types
- `babel.config.js`: Uses `@react-native/babel-preset`
- `metro.config.js`: Merges default `@react-native/metro-config`
- `jest.config.js`: Uses `@react-native/jest-preset`
- `.eslintrc.js`: Extends `@react-native` ruleset
- `.prettierrc.js`: Prettier formatting config

**Testing:**
- `__tests__/App.test.tsx`: Only test file; basic render assertion

## Naming Conventions

**Files:**
- React components: PascalCase with `.tsx` extension (e.g., `App.tsx`)
- Test files: `<ComponentName>.test.tsx` inside `__tests__/`
- Config files: lowercase with extension qualifier (e.g., `babel.config.js`, `metro.config.js`)

**Directories:**
- Tests: `__tests__/` at root level, double-underscore convention
- Native projects: lowercase platform name (`android/`, `ios/`)

**Components:**
- PascalCase function names exported as default (e.g., `export default App`)
- Co-located helper components in the same file when tightly coupled (e.g., `AppContent` inside `App.tsx`)

## Where to Add New Code

**New Screen or Feature Component:**
- Create `src/screens/` or `src/components/` directory (not yet present — establish at first feature)
- Implementation: `src/screens/MyScreen.tsx` or `src/components/MyComponent.tsx`
- Tests: `__tests__/MyScreen.test.tsx` or `__tests__/MyComponent.test.tsx`

**New Hook:**
- Create `src/hooks/` directory
- Implementation: `src/hooks/useMyHook.ts`

**Shared Utilities:**
- Create `src/utils/` directory
- Implementation: `src/utils/myUtil.ts`

**Navigation (when added):**
- Recommended location: `src/navigation/` for navigator definitions

**App-wide Context / State:**
- Recommended location: `src/store/` or `src/context/`

**Native Module (Android):**
- Kotlin source: `android/app/src/main/java/com/kindnessdice/`

**Native Module (iOS):**
- Swift source: `ios/KindnessDice/`

## Special Directories

**`node_modules/`:**
- Purpose: All npm dependencies
- Generated: Yes (via `npm install`)
- Committed: No

**`android/.gradle/`:**
- Purpose: Gradle build cache
- Generated: Yes
- Committed: No

**`.planning/`:**
- Purpose: GSD planning and codebase analysis documents
- Generated: By GSD tooling
- Committed: Yes

---

*Structure analysis: 2026-04-14*
