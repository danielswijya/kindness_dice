# Codebase Concerns

**Analysis Date:** 2026-04-14

## Tech Debt

**App is an unmodified React Native scaffold:**
- Issue: `App.tsx` still renders `NewAppScreen` (the default RN template screen) and contains no KindnessDice-specific logic. The project was initialized with `react-native init` and no application code has been written yet.
- Files: `App.tsx`, `index.js`
- Impact: The entire product feature set is absent. Every planned screen, component, game mechanic, and data layer still needs to be built from scratch.
- Fix approach: Replace `App.tsx` content with actual application UI, removing the `@react-native/new-app-screen` dependency once real screens exist.

**`@react-native/new-app-screen` scaffold dependency:**
- Issue: `@react-native/new-app-screen` (version `0.85.1`) is listed as a production dependency only to power the placeholder screen. It is not an application dependency.
- Files: `package.json`
- Impact: Shipping the app with this dependency wastes bundle size and signals the code was not cleaned up before development began.
- Fix approach: Remove from `dependencies` in `package.json` once `App.tsx` no longer imports `NewAppScreen`.

**Version pinned at `0.0.1` with no versioning strategy:**
- Issue: `versionCode 1` and `versionName "1.0"` in `android/app/build.gradle`, `version: "0.0.1"` in `package.json`. No automated bump process exists.
- Files: `package.json`, `android/app/build.gradle`
- Impact: Without a versioning convention, store releases and over-the-air update targeting will be error-prone.
- Fix approach: Document a semver bump process or add a script (e.g., `npm version`) to increment version, versionCode, and marketing version together.

## Security Considerations

**Release build signed with the debug keystore:**
- Risk: `android/app/build.gradle` release `buildType` sets `signingConfig signingConfigs.debug`, meaning any production APK/AAB is signed with the shared Android debug key (`android/app/debug.keystore`). Any actor with the debug key (which is the same across all RN projects by default) can sign a counterfeit APK that passes Android's signature verification as this app.
- Files: `android/app/build.gradle` (lines 100-106)
- Current mitigation: None. The comment in the file acknowledges this ("Caution! In production, you need to generate your own keystore file.") but the code has not been updated.
- Recommendations: Generate a dedicated release keystore, store credentials in a secrets manager or CI environment variables, and update the release `signingConfig` to reference it. Never commit the release keystore to the repository.

**Proguard disabled for release builds:**
- Risk: `enableProguardInReleaseBuilds = false` means the release APK ships unobfuscated Java/Kotlin bytecode. Business logic and any future server endpoints embedded in native code are trivially reversible.
- Files: `android/app/build.gradle` (line 60)
- Current mitigation: None (JS bundle is already minified by Hermes, but native code is not).
- Recommendations: Set `enableProguardInReleaseBuilds = true` and validate the app still launches after proguard transformation before first store submission.

**Empty `NSLocationWhenInUseUsageDescription` in Info.plist:**
- Risk: `ios/KindnessDice/Info.plist` declares `NSLocationWhenInUseUsageDescription` with an empty string. Apple requires a meaningful description; an empty string will likely cause App Store rejection and is a privacy red flag to users.
- Files: `ios/KindnessDice/Info.plist` (line 38)
- Current mitigation: None.
- Recommendations: Either remove the key entirely (if location is never used) or supply a meaningful user-facing explanation. Removing it is safest until location features are added.

## Fragile Areas

**Single test covers the entire app surface:**
- Files: `__tests__/App.test.tsx`
- Why fragile: The test simply renders `<App />` and asserts it does not throw. It will pass regardless of any broken UI logic, missing data, or wrong output as long as the component tree mounts without an exception.
- Safe modification: Add assertion-based tests for each new screen or component as they are added. Do not rely on this smoke test as evidence of correctness.
- Test coverage: Effectively zero functional coverage.

**No state management, routing, or persistence layer:**
- Files: `App.tsx`, `package.json`
- Why fragile: All future navigation, game state, and user progress must be built without an existing architecture to slot into. Early wrong choices (e.g., prop drilling vs. context vs. a state library) will be expensive to refactor.
- Safe modification: Establish navigation (e.g., React Navigation), state management, and storage patterns as the very first code added before feature work begins.

**`metro.config.js` has no customization:**
- Files: `metro.config.js`
- Why fragile: The config object is empty (`const config = {}`). Any future need for custom resolvers, asset extensions, or aliasing will require this file to be edited with no existing patterns to follow.
- Safe modification: Document the Metro customization strategy in a comment block when the first deviation from defaults is needed.

## Missing Critical Features

**No application code exists:**
- Problem: The project contains only the React Native CLI scaffold. No dice mechanic, kindness prompt content, UI screens, navigation, or data layer has been implemented.
- Blocks: All feature work, QA, and store submission.

**No CI/CD pipeline:**
- Problem: There is no `.github/workflows/`, `bitrise.yml`, or equivalent. No automated build, lint, type-check, or test runs on push.
- Blocks: Catching regressions early; automated store deployments.

**No type checking enforced in CI:**
- Problem: `package.json` scripts define `lint` but not a `typecheck` script (e.g., `tsc --noEmit`). TypeScript errors will only be caught locally if the developer runs the compiler manually.
- Blocks: Confidence in type safety as the codebase grows.
- Fix approach: Add `"typecheck": "tsc --noEmit"` to `package.json` scripts and run it in CI alongside `lint` and `test`.

## Test Coverage Gaps

**Only a mount smoke test exists:**
- What's not tested: All game logic, navigation flows, data persistence, UI interactions — none of which exist yet but will need tests when added.
- Files: `__tests__/App.test.tsx`
- Risk: Any future logic written without accompanying tests will accumulate silently.
- Priority: Low now (nothing to test), High once feature code is added.

## Dependencies at Risk

**`prettier` pinned to `2.8.8` while ecosystem has moved to v3:**
- Risk: Prettier 2.x is no longer receiving updates. New syntax features (e.g., TypeScript decorators, newer JS proposals) may not be formatted correctly.
- Impact: Formatting inconsistencies for newer syntax; eventual friction upgrading.
- Migration plan: Upgrade to `prettier` v3 and verify config compatibility (the `.prettierrc.js` options are v3-compatible).

**`eslint` pinned to `^8.19.0` (ESLint 8, pre-flat-config):**
- Risk: ESLint 9 introduced the flat config format as the default. ESLint 8 is in maintenance mode.
- Impact: The `@react-native` eslint config will eventually require a migration path to flat config.
- Migration plan: Upgrade to ESLint 9 and migrate `.eslintrc.js` to `eslint.config.js` when `@react-native/eslint-config` publishes v9-compatible config.

---

*Concerns audit: 2026-04-14*
