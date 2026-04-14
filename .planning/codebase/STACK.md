# Technology Stack

**Analysis Date:** 2026-04-14

## Languages

**Primary:**
- TypeScript 5.8.x - All application source code (`App.tsx`, `index.js`, `__tests__/`)
- JavaScript - Config files (`babel.config.js`, `metro.config.js`, `jest.config.js`, `.eslintrc.js`, `.prettierrc.js`)

**Secondary:**
- Ruby >= 2.6.10 - iOS CocoaPods dependency management (`Gemfile`, `ios/Podfile`)
- Kotlin 2.1.20 - Android native build support (`android/build.gradle`)

## Runtime

**Environment:**
- Node.js >= 22.11.0 (enforced via `engines` field in `package.json`)
- Hermes JS Engine (enabled on Android via `hermesEnabled=true` in `android/gradle.properties`)
- JSC (fallback on iOS if Hermes not configured)

**Package Manager:**
- npm
- Lockfile: `package-lock.json` present

## Frameworks

**Core:**
- React 19.2.3 - UI component model
- React Native 0.85.1 - Cross-platform mobile framework (iOS + Android)

**UI Utilities:**
- `react-native-safe-area-context` ^5.5.2 - Safe area insets for notches/home indicators (`App.tsx`)
- `@react-native/new-app-screen` 0.85.1 - Default scaffold screen component (`App.tsx`)

**Testing:**
- Jest ^29.6.3 - Test runner
- `@react-native/jest-preset` 0.85.1 - React Native Jest preset (`jest.config.js`)
- `react-test-renderer` 19.2.3 - React component renderer for tests (`__tests__/App.test.tsx`)
- `@types/jest` ^29.5.13 - Jest TypeScript types

**Build/Dev:**
- Metro 0.85.1 (via `@react-native/metro-config`) - JS bundler for React Native (`metro.config.js`)
- Babel with `@react-native/babel-preset` - JS/TS transpilation (`babel.config.js`)
- `@react-native-community/cli` 20.1.0 - React Native CLI tooling

**Linting/Formatting:**
- ESLint ^8.19.0 with `@react-native/eslint-config` 0.85.1 - Linting (`.eslintrc.js`)
- Prettier 2.8.8 - Code formatting (`.prettierrc.js`)

## Key Dependencies

**Critical:**
- `react-native` 0.85.1 - Core mobile framework; drives iOS/Android native layer
- `react` 19.2.3 - Component rendering engine; must stay in sync with `react-native` peer requirements
- `react-native-safe-area-context` ^5.5.2 - Required for correct layout on modern iOS/Android devices with notches

**Infrastructure:**
- `@react-native-community/cli-platform-android` 20.1.0 - Android build integration
- `@react-native-community/cli-platform-ios` 20.1.0 - iOS build integration
- `@react-native/typescript-config` 0.85.1 - Base TypeScript compiler config (extended by `tsconfig.json`)

## Configuration

**TypeScript:**
- `tsconfig.json` extends `@react-native/typescript-config`
- Additional `jest` types injected via `compilerOptions.types`
- Includes all `.ts` and `.tsx` files; excludes `node_modules` and `Pods`

**Build:**
- `babel.config.js` - Single preset: `module:@react-native/babel-preset`
- `metro.config.js` - Default Metro config with no custom overrides
- `jest.config.js` - Single preset: `@react-native/jest-preset`

**Android Build:**
- `android/build.gradle` - Build tools 36.0.0, minSdk 24, compileSdk/targetSdk 36, NDK 27.1.12297006
- `android/gradle.properties` - New Architecture enabled (`newArchEnabled=true`), Hermes enabled, supports armeabi-v7a/arm64-v8a/x86/x86_64

**iOS Build:**
- `ios/Podfile` - CocoaPods managed; uses `use_native_modules!` and `use_react_native!`
- `Gemfile` - CocoaPods >= 1.13 (excluding 1.15.0 and 1.15.1), xcodeproj < 1.26.0

**Prettier settings (`.prettierrc.js`):**
- `singleQuote: true`
- `trailingComma: 'all'`
- `arrowParens: 'avoid'`

## Platform Requirements

**Development:**
- Node.js >= 22.11.0
- Ruby >= 2.6.10 (for iOS CocoaPods)
- Android SDK with build tools 36.0.0, NDK 27.1.12297006
- Xcode (for iOS builds)
- CocoaPods >= 1.13

**Production:**
- iOS (minimum version determined by `min_ios_version_supported` from React Native 0.85.1 - typically iOS 15.1+)
- Android minSdkVersion 24 (Android 7.0+), targetSdkVersion 36

---

*Stack analysis: 2026-04-14*
